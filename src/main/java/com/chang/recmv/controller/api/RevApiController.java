package com.chang.recmv.controller.api;

/*import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
*/
///import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;

import com.chang.recmv.controller.RevController;
import com.chang.recmv.model.Rev;
import com.chang.recmv.service.RevService;
//import com.google.gson.JsonObject;

@RestController
@RequestMapping("/api/rev/*")
public class RevApiController {
	private static final Logger logger = LoggerFactory.getLogger(RevController.class); 

	@Autowired
	private RevService service;
	
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 리뷰쓰기 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	@GetMapping("/ckDupRev")
	public ResponseEntity<Integer> ckDupRev(@RequestParam("userNum") Integer userNum, @RequestParam("movieNum") Integer movieNum) throws Exception {
		logger.info("Rev: ckDupRev(@RequestParam(\"userNum\") Integer userNum, @RequestParam(\"movieNum\") Integer movieNum) 시작");
		Integer num = service.ckDupRev(userNum, movieNum);
		if(num == null)
			num = 0;
		logger.info("리뷰번호: " + num);
		logger.info("Rev: ckDupRev(@RequestParam(\"userNum\") Integer userNum, @RequestParam(\"movieNum\") Integer movieNum) 끝");
		
		return new ResponseEntity<Integer>(num, HttpStatus.OK);	
	}

	@PostMapping("/write")
	public ResponseEntity<String> writePOST(@RequestBody Rev rev) throws Exception {
		logger.info("Rev: writePOST(@RequestBody Rev rev) 시작");
		logger.info("리뷰쓰기: " + rev);	
		String movie = rev.getMovie();
		// 영화제목에 해당하는 리뷰의 개수
		Integer num = service.getNumRevsByTitle(movie);
		// 영화제목으로 이전 평균 평점 
		Double rating = service.getAvgRating(movie);
		// 1번째 리뷰이면 0.0으로 초기화
		if(rating < 0.0)
			rating = 0.0;
		// 리뷰개수를 곱해서 평점의 총합
		Double totalRating = rating * num;
		// 평점 총합 업데이트
		totalRating += rev.getRating();
		// 새로운 평점
		rating = totalRating / (num + 1);	
		service.updateAvgRating(movie, rating);
		service.writeRev(rev);
		logger.info("Rev: writePOST(@RequestBody Rev rev) 끝");
		
		return new ResponseEntity<String>("success", HttpStatus.OK);
	}
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 리뷰쓰기 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 리뷰수정 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	@PutMapping("/update/{num}")
	public ResponseEntity<String> updateRevPUT(@RequestBody Rev rev, @PathVariable Integer num) throws Exception {
		logger.info("Rev: updateRevPUT(@RequestBody Rev rev, @PathVariable Integer num) 시작");
		logger.info("리뷰수정 후: " + rev);	
		String movie = rev.getMovie();
		// 영화제목에 해당하는 리뷰의 개수
		Integer numRevs = service.getNumRevsByTitle(movie);
		// 영화제목으로 이전 평균 평점 
		Double rating = service.getAvgRating(movie);
		logger.info("평점: " + rating);
		// 리뷰개수를 곱해서 평점의 총합
		Double totalRating = rating * numRevs;
		// 평점 총합 업데이트
		totalRating -= service.readRating(num);
		totalRating += rev.getRating();
		// 새로운 평점
		rating = totalRating / numRevs;	
		service.updateAvgRating(movie, rating);		
		service.updateRev(rev, num);
		logger.info("Rev: updateRevPUT(@RequestBody Rev rev, @PathVariable Integer num) 끝");
		
		return new ResponseEntity<String>("success", HttpStatus.OK);
	}
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 리뷰수정 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 리뷰삭제 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	@DeleteMapping("/delete/{num}")
	public ResponseEntity<String> deleteRevDELETE(@PathVariable Integer num) throws Exception {
		logger.info("Rev: deleteRevDELETE(@PathVariable Integer num) 시작");
		logger.info("리뷰삭제: " + num);	
		// 삭제할 리뷰의 평점
		Rev rev = service.readRev(num);
		Double ratingDel = rev.getRating();
		// 삭제할 리뷰의 영화제목
		String movie = rev.getMovie();
		// 영화제목으로 이전 평균값 
		Double rating = service.getAvgRating(movie);
		// 영화제목에 해당하는 리뷰의 개수
		Integer numRevs = service.getNumRevsByTitle(movie);
		// 리뷰개수를 곱해서 총값
		Double totalRating = rating * numRevs;
		// 평균 평점 업데이트
		totalRating -= ratingDel;
		// 새로운 평균값
		if((numRevs - 1) > 1) rating = totalRating / (numRevs - 1);
		// 리뷰가 1개면 내림
		else if((numRevs - 1) == 1) rating = Math.floor(totalRating);
		// 리뷰가 0개면 -1로 초기화
		else rating = -1.0;
		logger.info("새로운 평균값: " + rating);
		service.updateAvgRating(movie, rating);		
		service.deleteRev(num);
		logger.info("Rev: deleteRevDELETE(@PathVariable Integer num) 끝");
		
		return new ResponseEntity<String>("success", HttpStatus.OK);
	}
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 리뷰삭제 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@	

	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 좋아요 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@	
	@GetMapping("/ckDupLike")
	public ResponseEntity<Integer> ckDupLike(@RequestParam("userNum") Integer userNum, @RequestParam("revNum") Integer revNum) throws Exception {
		logger.info("Rev: ckDupLike(@PathVariable Integer num, @RequestParam(\"userNum\") Integer userNum, @RequestParam(\"revNum\") Integer revNum) 시작");
		Integer likeNum = service.ckDupLike(userNum, revNum);
		if(likeNum == null)
			likeNum = 0;
		logger.info("Rev: ckDupLike(@PathVariable Integer num, @RequestParam(\"userNum\") Integer userNum, @RequestParam(\"revNum\") Integer revNum) 끝");

		return new ResponseEntity<Integer>(likeNum, HttpStatus.OK);	
	}	
	
	@PostMapping("/like")
	public ResponseEntity<String> likeRev(@RequestParam("userNum") Integer userNum, @RequestParam("revNum") Integer revNum) throws Exception {
		logger.info("Rev: likeRev(@PathVariable Integer num, @RequestParam(\"userNum\") Integer userNum, @RequestParam(\"revNum\") Integer revNum) 끝");
		service.addLike(userNum, revNum);
		logger.info("Rev: likeRev(@PathVariable Integer num, @RequestParam(\"userNum\") Integer userNum, @RequestParam(\"revNum\") Integer revNum) 끝");

		return new ResponseEntity<String>("success", HttpStatus.OK);
	}
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 좋아요 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@	
	
	/*
	@PostMapping(value = "/uploadSummernoteImageFile", produces = "application/json; charset=UTF-8")
	public JsonObject uploadSummernoteImageFile(@RequestParam("file") MultipartFile multipartFile) {
		logger.info("Rev: uploadSummernoteImageFile(@RequestParam(\"file\") MultipartFile multipartFile) 시작");
		
		JsonObject jsonObject = new JsonObject();
		
		String fileRoot = "D:\\summernote_image\\";
		
		String originalFileName = multipartFile.getOriginalFilename();
		String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
		
		String savedFileName = UUID.randomUUID() + extension;
		
		File targetFile = new File(fileRoot + savedFileName);
		
		try {
			InputStream fileStream = multipartFile.getInputStream();
			FileUtils.copyInputStreamToFile(fileStream, targetFile);
			jsonObject.addProperty("url", "/recmv/summernoteImage/" + savedFileName);
			jsonObject.addProperty("responseCode", "success");
		} catch(IOException e) {
			FileUtils.deleteQuietly(targetFile);
			jsonObject.addProperty("responseCode", "error");
			e.printStackTrace();
		}
		logger.info("Rev: uploadSummernoteImageFile(@RequestParam(\"file\") MultipartFile multipartFile) 끝");

		return jsonObject;
	}
	*/
}