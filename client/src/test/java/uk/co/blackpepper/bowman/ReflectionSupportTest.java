package uk.co.blackpepper.bowman;

import java.lang.reflect.Method;
import java.util.List;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import uk.co.blackpepper.bowman.annotation.LinkedResource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReflectionSupportTest {

	private ExpectedException thrown = ExpectedException.none();
	
	@Rule
	public ExpectedException getThrown() {
		return thrown;
	}
	
	@Test
	public void getIdWhenNoIdAccessor() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("No @ResourceId found for java.lang.Object");

		ReflectionSupport.getId(new Object());
	}
	
	@Test
	public void getAllLinkedResourceMethods() {
		List<Method> methodsAnnotatedWith = ReflectionSupport
			.getMethodsAnnotatedWith(TestEntity.class, LinkedResource.class);
		
		assertThat(methodsAnnotatedWith.size(), is(1));
		assertThat(methodsAnnotatedWith.get(0).getName(), is("getLinkedEntity"));
	}
	
	@Test
	public void getAllLinkedResourceMethodsWithinHierarchy() {
		List<Method> methodsAnnotatedWith = ReflectionSupport
			.getMethodsAnnotatedWith(TestEntityChild.class, LinkedResource.class);
		
		assertThat(methodsAnnotatedWith.size(), is(1));
		assertThat(methodsAnnotatedWith.get(0).getName(), is("getLinkedEntity"));
	}
	
	class TestEntity {
		
		@LinkedResource
		public Object getLinkedEntity(){
			return new Object();
		}
	}
	
	class TestEntityChild extends TestEntity {
	
	}
}
