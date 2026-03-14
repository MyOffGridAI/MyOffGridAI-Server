package com.myoffgridai.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_withDataOnly_shouldSetFieldsCorrectly() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
    }

    @Test
    void success_withDataAndMessage_shouldSetBoth() {
        ApiResponse<String> response = ApiResponse.success("data", "ok");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getMessage()).isEqualTo("ok");
    }

    @Test
    void error_shouldSetFieldsCorrectly() {
        ApiResponse<Object> response = ApiResponse.error("something went wrong");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("something went wrong");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getRequestId()).isNotNull();
    }

    @Test
    void paginated_shouldIncludePaginationMetadata() {
        ApiResponse<String> response = ApiResponse.paginated("page-data", 100L, 2, 20);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("page-data");
        assertThat(response.getTotalElements()).isEqualTo(100L);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(20);
    }

    @Test
    void requestId_shouldBeUniquePerInstance() {
        ApiResponse<String> r1 = ApiResponse.success("a");
        ApiResponse<String> r2 = ApiResponse.success("b");

        assertThat(r1.getRequestId()).isNotEqualTo(r2.getRequestId());
    }
}
