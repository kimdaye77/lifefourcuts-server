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

	//īī�� �α��� ���μ��� ����(���� ��ǥ�� Firebase CustomToken ����)
	public Map<String, Object> execKakaoLogin(String authorize_code) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();

		//1. ��������ū �ޱ�
		String accessToken = getAccessToken(authorize_code, "sign_in");
		System.out.println(accessToken);
		result.put("accessToken", accessToken.toString());
		//2. ����� ���� �о����
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
			//POST ��û�� ���� �⺻���� false�� setDoOutput�� true��
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			//POST ��û�� �ʿ�� �䱸�ϴ� �Ķ���� ��Ʈ���� ���� ����
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			StringBuilder sb = new StringBuilder();
			sb.append("grant_type=authorization_code");
			sb.append("&client_id=148faed447a0971199aff197f0c37b49"); //������ �߱޹��� key
			//���� redirect_uri�� īī���� ��ϵ� ��θ� �ۼ��ϸ� �ȴ�.
			sb.append("&redirect_uri=http://192.168.56.1:8080/kakao/"+stat); // ������ ������ ���� ���
			sb.append("&code="+authorize_code);
			bw.write(sb.toString());
			bw.flush();
			//��� �ڵ尡 200�̶�� ����
			int responseCode = conn.getResponseCode();
			System.out.println(sb.toString());
			System.out.println(conn.getResponseMessage());
			System.out.println("responseCode : " + responseCode);
			//��û�� ���� ���� JSONŸ���� Response �޼��� �о����
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = "";
			String result = "";
			
			while((line = br.readLine()) != null ) {
				result+=line;
			}
			System.out.println("response body : " + result);
			//gson ���̺귯���� ���Ե� Ŭ������ JSON�Ľ� ��ü ����
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
		//��û�ϴ� Ŭ���̾�Ʈ���� ���� ������ �ٸ� �� �ֱ⿡ HashMapŸ������ ���� 
        Map<String, Object> userInfo = new HashMap<>(); 
        String reqURL = "https://kapi.kakao.com/v2/user/me"; 
        try { 
            URL url = new URL(reqURL); 
            HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 
            conn.setRequestMethod("GET"); 
            //��û�� �ʿ��� Header�� ���Ե� ���� 
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
		//��������ū �ޱ�
		String access_Token = getAccessToken(code, "sign_out");
		System.out.println(access_Token);
		try {
			URL url = new URL(reqURL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			//POST ��û�� ���� �⺻���� false�� setDoOutput�� true��
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			//��û�� �ʿ��� Header�� ���Ե� ����
			conn.setRequestProperty("Authorization", "Bearer " + access_Token);
			
			//��� �ڵ尡 200�̶�� ����
			int responseCode = conn.getResponseCode();
			System.out.println(responseCode);
			// ��û�� ���� ���� JSONŸ���� Response �޼��� �о����
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

	//�⺻ ������ ��ȿ�Ⱓ�� 1�ð� �̸� ���� ������ �̿��ؼ� ������ �� �ִ� ����� ����� ����.
	public String createFirebaseCustomToken(Map<String,Object> userInfo) throws Exception {
		UserRecord userRecord;
		String uid = userInfo.get("id").toString();
		String email = userInfo.get("email").toString();
		String displayName = userInfo.get("nickname").toString();
		System.out.println(uid);

		//1. ����� ������ ���̾� ���̽� �������� update, ����� ������ �ִٸ� userRecord�� ���� ������ ����.
		try {
			UpdateRequest request = new UpdateRequest(uid);
			request.setEmail(email);
			request.setDisplayName(displayName);
			userRecord = FirebaseAuth.getInstance().updateUser(request);
		//1-2. ����� ������ ���ٸ� > catch ���п��� createUser�� ����ڸ� �����Ѵ�.
		} catch (FirebaseAuthException e) {
			CreateRequest createRequest = new CreateRequest();
			createRequest.setUid(uid);
			createRequest.setEmail(email);
			createRequest.setEmailVerified(false);
			createRequest.setDisplayName(displayName);

			userRecord = FirebaseAuth.getInstance().createUser(createRequest);
		}

		//2. ���޹��� user ������ CustomToken�� �����Ѵ�.
		return FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());
		}
}