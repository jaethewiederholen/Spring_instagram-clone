package cloneproject.Instagram.controller;

import cloneproject.Instagram.config.CustomValidator;
import cloneproject.Instagram.dto.post.*;
import cloneproject.Instagram.dto.result.ResultResponse;
import cloneproject.Instagram.service.PostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

import java.util.List;

import static cloneproject.Instagram.dto.result.ResultCode.*;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Api(tags = "게시물 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CustomValidator validator;

    @ApiOperation(value = "게시물 생성")
    @ApiImplicitParam(name = "content", value = "게시물 내용", example = "안녕하세요.", required = true)
    @PostMapping("/posts")
    public ResponseEntity<ResultResponse> createPost(
            @Validated @Length(max = 2200, message = "최대 2,200자까지 입력 가능합니다.")
            @RequestParam String content) {
        final Long postId = postService.create(content);
        final PostCreateResponse response = new PostCreateResponse(postId);

        return ResponseEntity.ok(ResultResponse.of(CREATE_POST_SUCCESS, response));
    }

    @ApiOperation(value = "게시물 이미지 업로드", consumes = MULTIPART_FORM_DATA_VALUE)
    @ApiImplicitParam(name = "id", value = "게시물 PK", example = "1", required = true)
    @PostMapping(value = "/posts/images", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultResponse> uploadImages(
            @Validated @NotNull(message = "게시물 PK는 필수입니다.")
            @RequestParam Long id,
            @RequestParam MultipartFile[] uploadImages) {
        final List<Long> imageIdList = postService.uploadImages(id, uploadImages);
        final PostImageUploadResponse response = new PostImageUploadResponse(imageIdList);

        return ResponseEntity.ok(ResultResponse.of(UPLOAD_POST_IMAGES_SUCCESS, response));
    }

    @ApiOperation(value = "게시물 이미지 태그 적용")
    @PostMapping(value = "/posts/images/tags")
    public ResponseEntity<ResultResponse> uploadImageTags(@RequestBody List<PostImageTagRequest> requests, BindingResult bindingResult) throws BindException {
        validator.validate(requests, bindingResult);
        if (bindingResult.hasErrors())
            throw new BindException(bindingResult);

        postService.addTags(requests);

        return ResponseEntity.ok(ResultResponse.of(ADD_POST_IMAGE_TAGS_SUCCESS, null));
    }

    @ApiOperation(value = "게시물 목록 조회")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "size", value = "한 페이지당 가져올 게시물 size", example = "5", required = true),
            @ApiImplicitParam(name = "page", value = "게시물 page", example = "1", required = true)
    })
    @GetMapping("/posts")
    public ResponseEntity<ResultResponse> getPosts(
            @Validated @NotNull(message = "조회할 게시물 size는 필수입니다.") @RequestParam int size,
            @Validated @NotNull(message = "조회할 게시물 page는 필수입니다.") @RequestParam int page) {
        final Page<PostDTO> postPage = postService.getPostDtoPage(size, page);

        return ResponseEntity.ok(ResultResponse.of(FIND_POST_PAGE_SUCCESS, postPage));
    }

    @ApiOperation(value = "게시물 삭제")
    @DeleteMapping("/posts")
    public ResponseEntity<ResultResponse> deletePost(@Validated @NotNull(message = "삭제할 게시물 PK는 필수입니다.") @RequestParam Long postId) {
        postService.delete(postId);

        return ResponseEntity.ok(ResultResponse.of(DELETE_POST_SUCCESS, null));
    }

    /*// TODO: 게시물 목록 조회 쿼리 개선하면서 같이 구현하기
    @ApiOperation(value = "게시물 조회")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ResultResponse> getPost(@Validated @NotNull(message = "삭제할 게시물 PK는 필수입니다.") @PathVariable Long postId) {
        final PostResponse response = postService.getPost(postId);

        return ResponseEntity.ok(ResultResponse.of(FIND_POST_SUCCESS, response));
    }*/
}
