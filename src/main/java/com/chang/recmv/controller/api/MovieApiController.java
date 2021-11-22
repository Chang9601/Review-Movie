package com.chang.recmv.controller.api;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chang.recmv.model.Movie;
import com.chang.recmv.service.MovieService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@RestController
@RequestMapping("/api/movie/*")
public class MovieApiController {

	private static final Logger logger = LoggerFactory.getLogger(MovieApiController.class); 

	@Autowired
	private MovieService service;
	
	private String clientId = "xayyRvE7DP1vyw8ehkC8";	
	private String clientSecret = "mLATucrjt8";

	@GetMapping("/searchMovieAPI")
	public ResponseEntity<JSONArray> searchMovieAPI(@RequestParam("query") String query) throws Exception {
		logger.info("Movie: searchMovieAPI(@RequestParam(\"query\") String query) 시작");
		
		logger.info("영화이름: " + query);
		
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("검색어 인코딩 실패",e);
        }	
      
        String apiURL = "https://openapi.naver.com/v1/search/movie?query=" + query;    // json 결과

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);
        String responseBody = get(apiURL,requestHeaders);

        String json = responseBody;
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject)parser.parse(json);
        JSONArray items = (JSONArray)obj.get("items");      
        
		for(int i = 0; i < items.size(); i++) {
			Movie movie = new Movie();
			JSONObject tmp = (JSONObject)items.get(i);
			
			String title = (String)tmp.get("title");
			String link = (String)tmp.get("link");
			String image = (String)tmp.get("image");
			String cast = (String)tmp.get("actor");
			String plot = getPlot((String)tmp.get("link"));

			title = title.replace("<b>", "");
			title = title.replace("</b>", "");
			cast = cast.replace("|", ", ");	
			cast = cast.replaceAll("(, )$", "");
			
			// 영화 중복방지
			if(service.readMovie(title) == null) {				
				movie.setTitle(title);
				movie.setLink(link);
				movie.setImage(image);
				movie.setCast(cast);
				movie.setPlot(plot);
				service.addMovie(movie);
			}
		}
				
        logger.info("Movie: searchMovieAPI(@RequestParam(\"query\") String query) 끝");  
        return new ResponseEntity<JSONArray>(items, HttpStatus.OK);
	}
		
    private static String get(String apiUrl, Map<String, String> requestHeaders){
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }


            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 에러 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection connect(String apiUrl){
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private static String readBody(InputStream body){
        InputStreamReader streamReader = new InputStreamReader(body);


        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();


            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }


            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }	
     
    private static String getPlot(String URL) throws Exception {
    	Document doc = Jsoup.connect(URL).get();
    	Element text = doc.select("p.con_tx").first();
    	String plot = null;
    	if(text != null) plot = text.text();
    	
    	return plot;
    } 
}