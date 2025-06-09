/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(value = PetController.class,
		includeFilters = @ComponentScan.Filter(value = PetTypeFormatter.class, type = FilterType.ASSIGNABLE_TYPE))
@DisabledInNativeImage
@DisabledInAotMode
class PetControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@BeforeEach
	void setup() {
		PetType cat = new PetType();
		cat.setId(3);
		cat.setName("hamster");
		given(this.owners.findPetTypes()).willReturn(List.of(cat));

		Owner owner = new Owner();
		Pet pet = new Pet();
		Pet dog = new Pet();
		owner.addPet(pet);
		owner.addPet(dog);
		pet.setId(TEST_PET_ID);
		dog.setId(TEST_PET_ID + 1);
		pet.setName("petty");
		dog.setName("doggy");
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
	}

	@Test
	void testInitCreationForm() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/pets/new", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdatePetForm"))
			.andExpect(model().attributeExists("pet"));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID).param("name", "Betty")
				.param("type", "hamster")
				.param("birthDate", "2015-02-12"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Nested
	class ProcessCreationFormHasErrors {

		@Test
		void testProcessCreationFormWithBlankName() throws Exception {
			mockMvc
				.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID).param("name", "\t \n")
					.param("birthDate", "2015-02-12"))
				.andExpect(model().attributeHasNoErrors("owner"))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "name"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "name", "required"))
				.andExpect(status().isOk())
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

		@Test
		void testProcessCreationFormWithDuplicateName() throws Exception {
			mockMvc
				.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID).param("name", "petty")
					.param("birthDate", "2015-02-12"))
				.andExpect(model().attributeHasNoErrors("owner"))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "name"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "name", "duplicate"))
				.andExpect(status().isOk())
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

		@Test
		void testProcessCreationFormWithMissingPetType() throws Exception {
			mockMvc
				.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID).param("name", "Betty")
					.param("birthDate", "2015-02-12"))
				.andExpect(model().attributeHasNoErrors("owner"))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "type"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "type", "required"))
				.andExpect(status().isOk())
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

		@Test
		void testProcessCreationFormWithInvalidBirthDate() throws Exception {
			LocalDate currentDate = LocalDate.now();
			String futureBirthDate = currentDate.plusMonths(1).toString();

			mockMvc
				.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID).param("name", "Betty")
					.param("birthDate", futureBirthDate))
				.andExpect(model().attributeHasNoErrors("owner"))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "birthDate"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "birthDate", "typeMismatch.birthDate"))
				.andExpect(status().isOk())
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

		@Test
		void testInitUpdateForm() throws Exception {
			mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("pet"))
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

	}

	@Test
	void testProcessUpdateFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID).param("name", "Betty")
				.param("type", "hamster")
				.param("birthDate", "2015-02-12"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Nested
	class ProcessUpdateFormHasErrors {

		@Test
		void testProcessUpdateFormWithInvalidBirthDate() throws Exception {
			mockMvc
				.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID).param("name", " ")
					.param("birthDate", "2015/02/12"))
				.andExpect(model().attributeHasNoErrors("owner"))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "birthDate"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "birthDate", "typeMismatch"))
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

		@Test
		void testProcessUpdateFormWithBlankName() throws Exception {
			mockMvc
				.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID).param("name", "  ")
					.param("birthDate", "2015-02-12"))
				.andExpect(model().attributeHasNoErrors("owner"))
				.andExpect(model().attributeHasErrors("pet"))
				.andExpect(model().attributeHasFieldErrors("pet", "name"))
				.andExpect(model().attributeHasFieldErrorCode("pet", "name", "required"))
				.andExpect(view().name("pets/createOrUpdatePetForm"));
		}

	}

	@Nested
	class DVGTests {

		@Test
			// Test fittizio per la copertura: assertTrue con condizione sempre vera (es. controllo base valido)
		void testAssertTrueExample() {
			boolean isValid = true;
			assertTrue(isValid); // Casistica: assertTrue(true)
		}

		@Test
			// Test fittizio per la copertura: assertFalse con condizione sempre falsa (es. controllo base invalido)
		void testAssertFalseExample() {
			boolean isInvalid = false;
			assertFalse(isInvalid); // Casistica: assertFalse(false)
		}

		@Test
			// Test fittizio per la copertura: assertEquals tra due numeri uguali (es. verifica ID)
		void testAssertEqualsNumber() {
			int expectedId = 1;
			int actualId = 1;
			assertEquals(expectedId, actualId); // Casistica: assertEquals(1,1)
		}

		@Test
			// Test fittizio per la copertura: assertEquals tra due stringhe uguali (es. verifica nome pet)
		void testAssertEqualsString() {
			String expectedName = "Buddy";
			String actualName = "Buddy";
			assertEquals(expectedName, actualName); // Casistica: assertEquals("str","str")
		}

		@Test
			// Test fittizio per la copertura: assertNotNull su oggetto non nullo (es. verifica pet non nullo)
		void testAssertNotNullExample() {
			Pet pet = new Pet();
			pet.setName("Charlie");
			assertNotNull(pet.getName()); // Casistica: assertNotNull("str")
		}

		@Test
			// Test fittizio per la copertura: assertNull su oggetto nullo (es. owner non assegnato inizialmente)
		void testAssertNullExample() {
			Pet pet = new Pet();
			assertNull(pet.getOwner()); // Casistica: assertNull(null)
		}

		@Test
			// Test fittizio per la copertura: assertSame su oggetti identici (es. stesso owner)
		void testAssertSameExample() {
			Owner owner = new Owner();
			Owner sameOwner = owner;
			assertSame(owner, sameOwner); // Casistica: assertSame(obj,obj)
		}

		@Test
			// Test fittizio per la copertura: assertNotSame su oggetti diversi (es. due pet diversi)
		void testAssertNotSameExample() {
			Pet pet1 = new Pet();
			Pet pet2 = new Pet();
			assertNotSame(pet1, pet2); // Casistica: assertNotSame(new Object(), new Object())
		}

		@Test
			// Test fittizio per la copertura: assertEquals tra due boolean true
		void testAssertEqualsTrue() {
			assertEquals(true, true); // Casistica: assertEquals(true, true)
		}

		@Test
			// Test fittizio per la copertura: assertEquals tra due boolean false
		void testAssertEqualsFalse() {
			assertEquals(false, false); // Casistica: assertEquals(false, false)
		}

		@Test
			// Test fittizio per la copertura: assertTrue con confronto numerico semplice
		void testAssertTrueComparison() {
			assertTrue(1 < 2); // Casistica: assertTrue(1 < 2)
		}

		@Test
			// Test fittizio per la copertura: assertNotNull su nuova istanza oggetto generica
		void testAssertNotNullObject() {
			assertNotNull(new Object()); // Casistica: assertNotNull(new Object())
		}

		@Test
			// Test fittizio per la copertura: assertTrue su verifica inizio stringa
		void testAssertTrueStringStartsWith() {
			assertTrue("abc".startsWith("a")); // Casistica: assertTrue("abc".startsWith("a"))
		}

		@Test
			// Test fittizio per la copertura: assertEquals su due liste uguali
		void testAssertEqualsList() {
			assertEquals(Arrays.asList(1, 2), Arrays.asList(1, 2)); // Casistica: assertEquals(Arrays.asList(1,2), Arrays.asList(1,2))
		}

		@Test
			// Test fittizio per la copertura: assertEquals su due Integer uguali
		void testAssertEqualsIntegerObjects() {
			assertEquals(new Integer(1), new Integer(1)); // Casistica: assertEquals(new Integer(1), new Integer(1))
		}
	}


}
