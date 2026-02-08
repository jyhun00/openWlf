package aml.openwlf.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("MemberPageController 통합 테스트")
class MemberPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /member/register 회원가입 폼")
    class RegisterFormTest {

        @Test
        @DisplayName("회원가입 폼 페이지 정상 렌더링")
        void shouldRenderRegisterForm() throws Exception {
            mockMvc.perform(get("/member/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("member/register"))
                    .andExpect(model().attributeExists("memberRequest"));
        }
    }

    @Nested
    @DisplayName("POST /member/register 회원가입 처리")
    class RegisterSubmitTest {

        @Test
        @DisplayName("정상 회원가입 시 결과 페이지 렌더링")
        void shouldShowResultOnValidSubmission() throws Exception {
            mockMvc.perform(post("/member/register")
                            .param("name", "홍길동")
                            .param("dateOfBirth", "1990-01-15")
                            .param("nationality", "KR"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("member/result"))
                    .andExpect(model().attributeExists("result"))
                    .andExpect(content().string(containsString("필터링 결과")));
        }

        @Test
        @DisplayName("이름이 watchlist에 매칭되면 높은 점수 반환")
        void shouldReturnHighScoreForWatchlistMatch() throws Exception {
            mockMvc.perform(post("/member/register")
                            .param("name", "Kim Jong Un")
                            .param("dateOfBirth", "1984-01-08")
                            .param("nationality", "KP"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("member/result"))
                    .andExpect(content().string(containsString("회원가입 거부")));
        }

        @Test
        @DisplayName("이름 미입력 시 폼 페이지로 복귀")
        void shouldReturnToFormWhenNameEmpty() throws Exception {
            mockMvc.perform(post("/member/register")
                            .param("name", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name("member/register"));
        }

        @Test
        @DisplayName("위험도 낮은 이름은 가입 승인")
        void shouldApproveRegistrationForSafeName() throws Exception {
            mockMvc.perform(post("/member/register")
                            .param("name", "이영희")
                            .param("nationality", "KR"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("member/result"))
                    .andExpect(content().string(containsString("회원가입 승인")));
        }
    }
}
