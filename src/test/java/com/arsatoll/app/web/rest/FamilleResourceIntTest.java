package com.arsatoll.app.web.rest;

import com.arsatoll.app.ArsatollserviceApp;

import com.arsatoll.app.domain.Famille;
import com.arsatoll.app.repository.FamilleRepository;
import com.arsatoll.app.service.FamilleService;
import com.arsatoll.app.service.dto.FamilleDTO;
import com.arsatoll.app.service.mapper.FamilleMapper;
import com.arsatoll.app.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;


import static com.arsatoll.app.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the FamilleResource REST controller.
 *
 * @see FamilleResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ArsatollserviceApp.class)
public class FamilleResourceIntTest {

    private static final String DEFAULT_NOM_FAMILLE = "AAAAAAAAAA";
    private static final String UPDATED_NOM_FAMILLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_IMAGE_FAMIILE = "AAAAAAAAAA";
    private static final String UPDATED_IMAGE_FAMIILE = "BBBBBBBBBB";

    @Autowired
    private FamilleRepository familleRepository;

    @Autowired
    private FamilleMapper familleMapper;

    @Autowired
    private FamilleService familleService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restFamilleMockMvc;

    private Famille famille;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final FamilleResource familleResource = new FamilleResource(familleService);
        this.restFamilleMockMvc = MockMvcBuilders.standaloneSetup(familleResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Famille createEntity(EntityManager em) {
        Famille famille = new Famille()
            .nomFamille(DEFAULT_NOM_FAMILLE)
            .description(DEFAULT_DESCRIPTION)
            .imageFamiile(DEFAULT_IMAGE_FAMIILE);
        return famille;
    }

    @Before
    public void initTest() {
        famille = createEntity(em);
    }

    @Test
    @Transactional
    public void createFamille() throws Exception {
        int databaseSizeBeforeCreate = familleRepository.findAll().size();

        // Create the Famille
        FamilleDTO familleDTO = familleMapper.toDto(famille);
        restFamilleMockMvc.perform(post("/api/familles")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(familleDTO)))
            .andExpect(status().isCreated());

        // Validate the Famille in the database
        List<Famille> familleList = familleRepository.findAll();
        assertThat(familleList).hasSize(databaseSizeBeforeCreate + 1);
        Famille testFamille = familleList.get(familleList.size() - 1);
        assertThat(testFamille.getNomFamille()).isEqualTo(DEFAULT_NOM_FAMILLE);
        assertThat(testFamille.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testFamille.getImageFamiile()).isEqualTo(DEFAULT_IMAGE_FAMIILE);
    }

    @Test
    @Transactional
    public void createFamilleWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = familleRepository.findAll().size();

        // Create the Famille with an existing ID
        famille.setId(1L);
        FamilleDTO familleDTO = familleMapper.toDto(famille);

        // An entity with an existing ID cannot be created, so this API call must fail
        restFamilleMockMvc.perform(post("/api/familles")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(familleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Famille in the database
        List<Famille> familleList = familleRepository.findAll();
        assertThat(familleList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllFamilles() throws Exception {
        // Initialize the database
        familleRepository.saveAndFlush(famille);

        // Get all the familleList
        restFamilleMockMvc.perform(get("/api/familles?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(famille.getId().intValue())))
            .andExpect(jsonPath("$.[*].nomFamille").value(hasItem(DEFAULT_NOM_FAMILLE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].imageFamiile").value(hasItem(DEFAULT_IMAGE_FAMIILE.toString())));
    }
    
    @Test
    @Transactional
    public void getFamille() throws Exception {
        // Initialize the database
        familleRepository.saveAndFlush(famille);

        // Get the famille
        restFamilleMockMvc.perform(get("/api/familles/{id}", famille.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(famille.getId().intValue()))
            .andExpect(jsonPath("$.nomFamille").value(DEFAULT_NOM_FAMILLE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.imageFamiile").value(DEFAULT_IMAGE_FAMIILE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingFamille() throws Exception {
        // Get the famille
        restFamilleMockMvc.perform(get("/api/familles/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateFamille() throws Exception {
        // Initialize the database
        familleRepository.saveAndFlush(famille);

        int databaseSizeBeforeUpdate = familleRepository.findAll().size();

        // Update the famille
        Famille updatedFamille = familleRepository.findById(famille.getId()).get();
        // Disconnect from session so that the updates on updatedFamille are not directly saved in db
        em.detach(updatedFamille);
        updatedFamille
            .nomFamille(UPDATED_NOM_FAMILLE)
            .description(UPDATED_DESCRIPTION)
            .imageFamiile(UPDATED_IMAGE_FAMIILE);
        FamilleDTO familleDTO = familleMapper.toDto(updatedFamille);

        restFamilleMockMvc.perform(put("/api/familles")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(familleDTO)))
            .andExpect(status().isOk());

        // Validate the Famille in the database
        List<Famille> familleList = familleRepository.findAll();
        assertThat(familleList).hasSize(databaseSizeBeforeUpdate);
        Famille testFamille = familleList.get(familleList.size() - 1);
        assertThat(testFamille.getNomFamille()).isEqualTo(UPDATED_NOM_FAMILLE);
        assertThat(testFamille.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testFamille.getImageFamiile()).isEqualTo(UPDATED_IMAGE_FAMIILE);
    }

    @Test
    @Transactional
    public void updateNonExistingFamille() throws Exception {
        int databaseSizeBeforeUpdate = familleRepository.findAll().size();

        // Create the Famille
        FamilleDTO familleDTO = familleMapper.toDto(famille);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFamilleMockMvc.perform(put("/api/familles")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(familleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Famille in the database
        List<Famille> familleList = familleRepository.findAll();
        assertThat(familleList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteFamille() throws Exception {
        // Initialize the database
        familleRepository.saveAndFlush(famille);

        int databaseSizeBeforeDelete = familleRepository.findAll().size();

        // Delete the famille
        restFamilleMockMvc.perform(delete("/api/familles/{id}", famille.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Famille> familleList = familleRepository.findAll();
        assertThat(familleList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Famille.class);
        Famille famille1 = new Famille();
        famille1.setId(1L);
        Famille famille2 = new Famille();
        famille2.setId(famille1.getId());
        assertThat(famille1).isEqualTo(famille2);
        famille2.setId(2L);
        assertThat(famille1).isNotEqualTo(famille2);
        famille1.setId(null);
        assertThat(famille1).isNotEqualTo(famille2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(FamilleDTO.class);
        FamilleDTO familleDTO1 = new FamilleDTO();
        familleDTO1.setId(1L);
        FamilleDTO familleDTO2 = new FamilleDTO();
        assertThat(familleDTO1).isNotEqualTo(familleDTO2);
        familleDTO2.setId(familleDTO1.getId());
        assertThat(familleDTO1).isEqualTo(familleDTO2);
        familleDTO2.setId(2L);
        assertThat(familleDTO1).isNotEqualTo(familleDTO2);
        familleDTO1.setId(null);
        assertThat(familleDTO1).isNotEqualTo(familleDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(familleMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(familleMapper.fromId(null)).isNull();
    }
}
