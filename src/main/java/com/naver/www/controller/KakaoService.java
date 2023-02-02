package com.naver.www.controller;

import java.io.BufferedReader;    
import java.io.BufferedWriter;   
import java.io.IOException;     
import java.io.InputStreamReader;     
import java.io.OutputStreamWriter;      
import java.net.HttpURLConnection;     
import java.net.URL;    
import java.util.HashMap;      
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import com.google.gson.JsonElement;    
import com.google.gson.JsonObject;     
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class KakaoService {

	//카카오 로그인 프로세스 진행(최종 목표는 Firebase CustomToken 발행)
	public Map<String, Object> execKakaoLogin(String authorize_code) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();

		//1. 엑세스토큰 받기
		String accessToken = getAccessToken(authorize_code, "sign_in");
		System.out.println(accessToken);
		result.put("accessToken", accessToken.toString());
		//2. 사용자 정보 읽어오기
		Map<String, Object> userInfo = getUserInfo(accessToken);
		result.put("userInfo", userInfo.toString());
		System.out.println("=====");
		System.out.println(userInfo.toString());
		try{
			System.out.println("1=====");
			result.put("customToken", createFirebaseCustomToken(userInfo).toString());
			result.put("errYn", "N");
			result.put("errMsg", "No error");
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			result.put("errYn", "Y");
			result.put("errMsg", "Exception : " + e.getMessage());
		}
		
		System.out.println(result);

		return result;
	}

	public String getAccessToken(String authorize_code, String stat) {
		String access_Token = "";
		String refresh_Token = "";
		String reqURL = "https://kauth.kakao.com/oauth/token";
		
		try {
			URL url = new URL(reqURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//POST 요청을 위해 기본값이 false인 setDoOutput을 true로
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			//POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			StringBuilder sb = new StringBuilder();
			sb.append("grant_type=authorization_code");
			sb.append("&client_id=148faed447a0971199aff197f0c37b49"); //본인이 발급받은 key
			//설정 redirect_uri는 카카오에 등록된 경로를 작성하면 된다.
			sb.append("&redirect_uri=http://192.168.56.1:8080/kakao/"+stat); // 본인이 설정해 놓은 경로
			sb.append("&code="+authorize_code);
			bw.write(sb.toString());
			bw.flush();
			//결과 코드가 200이라면 성공
			int responseCode = conn.getResponseCode();
			System.out.println(sb.toString());
			System.out.println(conn.getResponseMessage());
			System.out.println("responseCode : " + responseCode);
			//요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = "";
			String result = "";
			
			while((line = br.readLine()) != null ) {
				result+=line;
			}
			System.out.println("response body : " + result);
			//gson 라이브러리에 포함된 클래스로 JSON파싱 객체 생성
			JsonElement element = JsonParser.parseString(result);
			access_Token = element.getAsJsonObject().get("access_token").getAsString();
			refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();
			br.close();
			bw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
		return access_Token;
	}
	public Map<String, Object> getUserInfo(String access_Token) {
		//요청하는 클라이언트마다 가진 정보가 다를 수 있기에 HashMap타입으로 선언 
        Map<String, Object> userInfo = new HashMap<>(); 
        String reqURL = "https://kapi.kakao.com/v2/user/me"; 
        try { 
            URL url = new URL(reqURL); 
            HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 
            conn.setRequestMethod("GET"); 
            //요청에 필요한 Header에 포함될 내용 
            conn.setRequestProperty("Authorization", "Bearer " + access_Token); 
            int responseCode = conn.getResponseCode(); 
            System.out.println("responseCode : " + responseCode);

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream())); 
            String line = ""; 
            String result = ""; 
            while ((line = br.readLine()) != null) { 
                result += line; 
            }
			System.out.println("response bdoy : " + result);
            JsonElement element = JsonParser.parseString(result); 
            String id = element.getAsJsonObject().get("id").getAsString(); 
            JsonObject properties = element.getAsJsonObject().get("properties").getAsJsonObject(); 
            JsonObject kakao_account = element.getAsJsonObject().get("kakao_account").getAsJsonObject(); 
            String nickname = properties.getAsJsonObject().get("nickname").getAsString(); 
            String email = kakao_account.getAsJsonObject().get("email").getAsString(); 
            userInfo.put("id", id); 
            userInfo.put("nickname", nickname); 
			userInfo.put("email", email); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
        return userInfo; 
    	
	}
	
	public String kakaoLogout(String code) {
		String reqURL = "https://kapi.kakao.com/v1/user/logout";
		//엑세스토큰 받기
		String access_Token = getAccessToken(code, "sign_out");
		System.out.println(access_Token);
		try {
			URL url = new URL(reqURL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			//POST 요청을 위해 기본값이 false인 setDoOutput을 true로
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			//요청에 필요한 Header에 포함될 내용
			conn.setRequestProperty("Authorization", "Bearer " + access_Token);
			
			//결과 코드가 200이라면 성공
			int responseCode = conn.getResponseCode();
			System.out.println(responseCode);
			// 요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = "";
			String result = "";
			System.out.println("response bdoy : " + result);

			while ((line = br.readLine()) != null) {
				result += line;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return access_Token;
	}

	//기본 적으로 유효기간은 1시간 이며 유저 정보를 이용해서 생성할 수 있는 방법이 어려개 있음.
	public String createFirebaseCustomToken(Map<String,Object> userInfo) throws Exception {
		UserRecord userRecord;
		String uid = userInfo.get("id").toString();
		String email = userInfo.get("email").toString();
		String displayName = userInfo.get("nickname").toString();
		System.out.println(uid);

		//1. 사용자 정보로 파이어 베이스 유저정보 update, 사용자 정보가 있다면 userRecord에 유저 정보가 담긴다.
		try {
			UpdateRequest request = new UpdateRequest(uid);
			request.setEmail(email);
			request.setDisplayName(displayName);
			userRecord = FirebaseAuth.getInstance().updateUser(request);
		//1-2. 사용자 정보가 없다면 > catch 구분에서 createUser로 사용자를 생성한다.
		} catch (FirebaseAuthException e) {
			CreateRequest createRequest = new CreateRequest();
			createRequest.setUid(uid);
			createRequest.setEmail(email);
			createRequest.setEmailVerified(false);
			createRequest.setDisplayName(displayName);

			userRecord = FirebaseAuth.getInstance().createUser(createRequest);
		}

		//2. 전달받은 user 정보로 CustomToken을 발행한다.
		return FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
		}
}