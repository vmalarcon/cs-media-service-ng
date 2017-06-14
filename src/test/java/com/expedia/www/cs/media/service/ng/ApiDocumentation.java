package com.expedia.www.cs.media.service.ng;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.expedia.www.cs.media.service.ng.controller.HelloController;

@RunWith(SpringRunner.class)
@WebMvcTest(HelloController.class)
@AutoConfigureRestDocs("target/generated-snippets")
public class ApiDocumentation {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void generateDocsForHelloController() throws Exception {
        this.mockMvc.perform(RestDocumentationRequestBuilders.get("/service/hello/{name}", "world"))
                .andExpect(status().isOk())
                .andDo(document("hello-controller", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("name").optional().description("Generate a message using name passed as parameter.")
                        ),
                        responseFields(
                                fieldWithPath("success").description("True if the service has responded without any errors, false otherwise."),
                                fieldWithPath("message").description("The generated message.")
                        )
                ));
    }
}
