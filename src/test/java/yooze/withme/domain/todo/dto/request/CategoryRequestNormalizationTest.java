package yooze.withme.domain.todo.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CategoryRequestNormalizationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void createRequestStripsSurroundingWhitespaceFromJson() throws Exception {
        CreateCategoryRequest request = objectMapper.readValue(
                "{\"categoryName\":\"  업무 \",\"categoryColor\":\" #FFAA03 \"}",
                CreateCategoryRequest.class
        );

        assertThat(request.categoryName()).isEqualTo("업무");
        assertThat(request.categoryColor()).isEqualTo("#FFAA03");
        assertThat(violationMessages(request)).isEmpty();
    }

    @Test
    void createRequestKeepsNullColor() {
        CreateCategoryRequest request = new CreateCategoryRequest(" 업무 ", null);

        assertThat(request.categoryName()).isEqualTo("업무");
        assertThat(request.categoryColor()).isNull();
    }

    @Test
    void createRequestRejectsWhitespaceOnlyName() {
        CreateCategoryRequest request = new CreateCategoryRequest("    ", null);

        assertThat(request.categoryName()).isEmpty();
        assertThat(violationMessages(request)).contains("카테고리 이름은 필수입니다.");
    }

    @Test
    void createRequestRejectsNameExceedingLimitAfterStrip() {
        String name = " " + "가".repeat(65) + " ";

        assertThat(violationMessages(new CreateCategoryRequest(name, null)))
                .contains("카테고리 이름은 64자를 초과할 수 없습니다.");
    }

    @Test
    void createRequestAcceptsNameWithinLimitOnlyBecauseOfStrip() {
        String name = "   " + "가".repeat(64) + "   ";

        assertThat(violationMessages(new CreateCategoryRequest(name, null))).isEmpty();
    }

    @Test
    void updateRequestStripsNameButKeepsNullAsUnchanged() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(1L, " 업무 ", null, null);

        assertThat(request.categoryName()).isEqualTo("업무");
        assertThat(request.categoryColor()).isNull();
        assertThat(violationMessages(request)).isEmpty();
    }

    @Test
    void updateRequestRejectsWhitespaceOnlyName() {
        UpdateCategoryRequest request = new UpdateCategoryRequest(1L, "   ", null, null);

        assertThat(violationMessages(request))
                .contains("카테고리 이름은 1자 이상 64자 이하여야 합니다.");
    }

    private <T> Set<String> violationMessages(T request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
