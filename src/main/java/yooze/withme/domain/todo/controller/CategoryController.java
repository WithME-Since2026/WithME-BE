package yooze.withme.domain.todo.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.todo.controller.docs.CategoryControllerDocs;
import yooze.withme.domain.todo.dto.request.CreateCategoryRequest;
import yooze.withme.domain.todo.dto.request.UpdateCategoryRequest;
import yooze.withme.domain.todo.dto.response.CategoryDetailResponse;
import yooze.withme.domain.todo.dto.response.CategoryResponse;
import yooze.withme.domain.todo.service.CategoryCommandService;
import yooze.withme.domain.todo.service.CategoryQueryService;

// TODO: 이 클래스의 Long.parseLong(userDetails.getUsername()) 패턴은 TodoController에서 시작한
// userId 전달 방식(전역 통일 예정)으로 추후 함께 변경 필요.
@RestController
@RequestMapping("/api/v1/todo/category")
@RequiredArgsConstructor
public class CategoryController implements CategoryControllerDocs {

    private final CategoryCommandService categoryCommandService;
    private final CategoryQueryService categoryQueryService;

    @Override
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        CategoryResponse response = categoryCommandService.createCategory(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_CATEGORY_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        CategoryResponse response = categoryCommandService.updateCategory(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_CATEGORY_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<CategoryDetailResponse>>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<CategoryDetailResponse> response = categoryQueryService.getCategories(userId);
        return ApiResponse.success(SuccessStatus.GET_CATEGORIES_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long categoryId
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        categoryCommandService.deleteCategory(userId, categoryId);
        return ApiResponse.success(SuccessStatus.DELETE_CATEGORY_SUCCESS);
    }
}
