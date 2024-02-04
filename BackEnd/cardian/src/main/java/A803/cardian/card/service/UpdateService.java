package A803.cardian.card.service;


/*
 *   작성자 : 정여민
 *   작성 일시 : 2024.02.02
 *   업데이트 : 2024.02.02
 *   내용 : 카드사에서 가져온 카드 정보 및 거래내역 우리 DB에 업데이트 하는 서비스
 *
 * */


import A803.cardian.card.domain.Card;
import A803.cardian.card.domain.MyCard;
import A803.cardian.card.domain.Transaction;
import A803.cardian.card.repository.CardRepository;
import A803.cardian.card.repository.MycardRepository;
import A803.cardian.card.repository.TransactionRepository;
import A803.cardian.member.domain.Member;
import A803.cardian.member.repository.MemberRepository;
import A803.cardian.util.WebClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.json.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpHeaders;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class UpdateService {

    @Autowired
    private final MemberRepository memberRepository;

    @Autowired
    private final WebClientService webClientService;

    @Autowired
    private final TransactionRepository transactionRepository;

    @Autowired
    private final CardRepository cardRepository;

    @Autowired
    private final MycardRepository mycardRepository;

    // 유저가 회원가입



    //1. 유저가 로그인 -> memberId -> MyCard 조회 -> 카드 아이디가 나옴
    //2. memberId로 카드사에서 유저의 카드 조회 -> Card, MyCard Table update
    //2-1. updateDate : 현재 시간으로 저장 < 필수 이거 없으면 3번이 안 돌아감!
    //
    //3. memberId로 카드사에서 유저의 거래내역 조회
    //-> 이때, Card의 updateDate보다 이전것만 가져오기 (아마 user의 update는 mycard에 다 있는데, 그 중 첫번째 걸 기준으로 하면 될 듯?

    // 내용 : 멤버 아이디를 입력받아, 거래 정보를 업데이트 하는 메서드
    @Transactional
    public boolean updateTransactions(Integer memberId){
        // 1. 마지막 업데이트 날짜 가져오기
        String updateDate = updateDate(memberId);

        // 카드사 API 사용
        String baseUrl = null;
        String path = null;

        // JSON 입력 받을 것
//        JSONParser jsonParser = new JSONParser();

        // 2. memberId로 카드사에서 거래 내역 가져오기
        baseUrl = "http://i10a803.p.ssafy.io:8082";
        path = "/transaction/1/".concat(updateDate);
        Map<String, Object> transactions = webClientService.get(baseUrl, path);

        // 거래 내역이 없으면
        if(transactions.size() == 0){
            return false;
        }


        // Map으로 받아온 것 JSON으로 변환
        JSONObject transactionObject = new JSONObject(transactions);

        // JSONObject 형태인 것에서 transactionList만 따로 받아오기
        JSONArray transactionJsonArray = (JSONArray) transactionObject.get("transactionList");

        // transaction Array 돌면서 거래 내역 뽑아오기
        for (Object o : transactionJsonArray) {
            // 거래내역 -> 카드아이디(카드사DB카드아이디) -> 카드에 가서 내 카드 가져오기
            // 거래내역 1개 가져오기
            System.out.println("o = " + o);
            JSONObject transaction = (JSONObject) o;

            System.out.println("transaction = " + transaction);

            System.out.println("transaction1 = " + transaction.get("id"));

            // 카드 정보
            JSONObject transactionCard = (JSONObject) transaction.get("card");
            int carddbID = transactionCard.getInt("id");
            System.out.println("carddbID = " + carddbID);
            Card card = cardRepository.findCardByCardDatabaseId(carddbID);

            MyCard myCard = mycardRepository.findByCard_Id(card.getId());



            Transaction newTransaction = Transaction.builder()
                    .myCard(myCard)
                    .number(transaction.getInt("number"))
                    .day(LocalDate.parse(transaction.getString("day")))
                    .date(LocalDateTime.parse(transaction.getString("date")))
                    .store(transaction.getString("store"))
                    .price(transaction.getInt("price"))
                    .status(transaction.getBoolean("status"))
                    .discount(transaction.getBoolean("discount"))
                    .build();


            System.out.println("newTransaction = " + newTransaction);

            // 거래 내역 저장하고
            transactionRepository.save(newTransaction);
        }

        // 거래 내역 최종 갱신일 업데이트
        memberRepository.updateMemberUpdateDate(memberId, LocalDateTime.now());


        return true;
    }


    /*
     * 업데이트 날짜 반환 메서드
     * 작성일시 : 2024.02.02
     * 업데이트 : 2024.02.04
     * 내용 : updateDate를 MyCard가 아닌 member에서 가져옴
     */
    public String updateDate(Integer memberId){

        Optional<Member> member = memberRepository.findById(memberId);

        String updateDate = String.valueOf(LocalDateTime.now());
        // 멤버가 존재하면
        if(member.isPresent()){
            // 카드사 서버에 쿼리하기 위한 형식으로 바꾸기 yyyy-MM-ddHH:mm:ss 형태
            String temp_updateDate = String.valueOf(member.get().getUpdateDate());
            String [] updateDate01 = temp_updateDate.split("T");
            updateDate = updateDate01[0].concat(updateDate01[1]).split("\\.")[0];
        }

        log.info(updateDate);

        return updateDate;
    }




}
