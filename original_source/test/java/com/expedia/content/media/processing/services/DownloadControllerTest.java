package com.expedia.content.media.processing.services;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DownloadControllerTest {

    private static final Resource FILE_RESOURCE = new ClassPathResource("/log4j.xml");

    private MockMvc mockMvc;

    @Mock
    private ResourcePatternResolver resourcePatternResolver;

    @Before
    public void setUp() throws Exception {
        DownloadController downloadController = new DownloadController();
        setFieldValue(downloadController, "resourcePatternResolver", resourcePatternResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(downloadController).build();
    }

    @Test
    public void testResourcesNotFound() throws Exception {
        when(resourcePatternResolver.getResources(anyString())).thenReturn(new Resource[0]);

        mockMvc.perform(
                get("/media/s3/download")
                        .param("url", "some url"))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    public void testTooManyResourcesFound() throws Exception {
        Resource[] resources = {createMockResource(true), createMockResource(true)};
        when(resourcePatternResolver.getResources(anyString()))
                .thenReturn(resources);

        mockMvc.perform(
                get("/media/s3/download")
                        .param("url", "some url"))
                .andExpect(status().is(HttpStatus.CONFLICT.value()));
    }

    @Test
    public void testOnlyOneResourceExists() throws Exception {
        Resource[] resources = {createMockResource(false), createMockResource(true)};
        when(resourcePatternResolver.getResources(anyString()))
                .thenReturn(resources);

        mockMvc.perform(
                get("/media/s3/download")
                        .param("url", "some url"))
                .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    public void testDownloadResource() throws Exception {
        when(resourcePatternResolver.getResources(anyString()))
                .thenReturn(new Resource[] {FILE_RESOURCE});

        mockMvc.perform(
                get("/media/s3/download")
                        .param("url", "some url"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    private Resource createMockResource(boolean exists) {
        Resource result = mock(Resource.class);
        when(result.exists()).thenReturn(exists);
        return result;
    }

}
