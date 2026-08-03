package yooze.withme.domain.todo.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.todo.dto.request.CreateCategoryRequest;
import yooze.withme.domain.todo.dto.request.UpdateCategoryRequest;
import yooze.withme.domain.todo.dto.response.CategoryDetailResponse;
import yooze.withme.domain.todo.dto.response.CategoryResponse;

@Tag(name = "카테고리", description = "Todo 카테고리 생성/수정/조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface CategoryControllerDocs {

    @Operation(summary = "카테고리 생성")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "카테고리 생성 성공"
    )
    @PostMapping
    ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody CreateCategoryRequest request
    );

    @Operation(summary = "카테고리 수정")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "카테고리 수정 성공"
    )
    @PatchMapping
    ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody UpdateCategoryRequest request
    );

    @Operation(summary = "카테고리 목록 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "카테고리 목록 조회 성공"
    )
    @GetMapping
    ResponseEntity<ApiResponse<List<CategoryDetailResponse>>> getCategories(
            @Parameter(hidden = true) UserDetails userDetails
    );
}
