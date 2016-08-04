package uk.co.blackpepper.sdrclient.gen;

import java.io.IOException;
import java.net.URI;

import javax.persistence.Id;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import uk.co.blackpepper.sdrclient.annotation.LinkedResource;
import uk.co.blackpepper.sdrclient.annotation.RemoteResource;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class GeneratorTest {

	private GeneratedClassWriter classWriter;

	private Generator generator;

	@Before
	public void setup() {
		classWriter = mock(GeneratedClassWriter.class);

		generator = new Generator();
	}

	@Test
	public void generateWithoutRestResourceAnnotationDoesNothing() throws IOException {
		JavaClassSource javaClass = Roaster.create(JavaClassSource.class)
			.setPackage("test")
			.setName("Entity");

		generator.generate(new RoasterClassSourceAdapter(javaClass), classWriter);

		verifyZeroInteractions(classWriter);
	}
	
	@Test
	public void generateWritesContent() throws IOException {
		JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
		javaClass.setPackage("test").setName("Entity").addAnnotation(RemoteResource.class)
				.setStringValue("/path/to/resource");
		javaClass.addField().setName("id").addAnnotation(Id.class);
		javaClass.addField().setName("name").setType(String.class);

		JavaClassSource output = generateAndParseContent(javaClass);
		
		assertThat("qualifiedName", output.getQualifiedName(), is("test.client.Entity"));
		assertThat("has class annotation", output.hasAnnotation(RemoteResource.class), is(true));
		assertThat("class annotation value", output.getAnnotation(RemoteResource.class).getStringValue(),
				is("/path/to/resource"));
		assertThat("has id annotation", output.getField("id").hasAnnotation(Id.class), is(false));
		assertThat("id field type", output.getField("id").getType().getQualifiedName(), is("java.net.URI"));
		assertThat("id getter", output.getMethod("getId"), is(notNullValue()));
		assertThat("id setter", output.getMethod("setId", URI.class), is(nullValue()));
		assertThat("name getter", output.getMethod("getName"), is(notNullValue()));
		assertThat("name setter", output.getMethod("setName", String.class), is(notNullValue()));
	}

	@Test
	public void generateWritesToRelativePath() throws IOException {
		JavaClassSource javaClass = createValidJavaClassSource("pkg.X");
		
		generator.generate(new RoasterClassSourceAdapter(javaClass), classWriter);
		
		verify(classWriter).write(eq("pkg/client/X.java"), anyString());
	}

	@Test
	public void generateWithClientAnnotationPreservesAnnotation() throws IOException {
		JavaClassSource javaClass = createValidJavaClassSource();
		javaClass.addField()
			.setName("field")
			.addAnnotation(LinkedResource.class);

		JavaClassSource output = generateAndParseContent(javaClass);
		
		assertThat("field getter has @LinkedResource", output.getMethod("getField").hasAnnotation(LinkedResource.class),
				is(true));
	}

	@Test
	public void generateWithOtherAnnotationDiscardsAnnotation() throws IOException {
		JavaClassSource javaClass = createValidJavaClassSource();
		javaClass.addField()
			.setName("field")
			.addAnnotation(Deprecated.class);

		JavaClassSource output = generateAndParseContent(javaClass);
		
		assertThat("field getter has @Deprecated", output.getMethod("getField").hasAnnotation(Deprecated.class),
				is(false));
	}
	
	@Test
	public void generateWithClientResourceTypeConvertsType() throws IOException {
		JavaClassSource javaClass = createValidJavaClassSource("sourcepackage.X");
		javaClass.addField()
			.setName("field")
			.setType("sourcepackage.Y");
		
		JavaClassSource output = generateAndParseContent(javaClass);
		
		assertThat("field type", output.getField("field").getType().getQualifiedName(), is("sourcepackage.client.Y"));
	}
	
	private JavaClassSource generateAndParseContent(JavaClassSource in) throws IOException {
		ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);

		generator.generate(new RoasterClassSourceAdapter(in), classWriter);

		verify(classWriter).write(anyString(), content.capture());

		return (JavaClassSource) Roaster.parse(content.getValue());
	}
	
	private static JavaClassSource createValidJavaClassSource() {
		return createValidJavaClassSource("packageName.ClassName");
	}

	private static JavaClassSource createValidJavaClassSource(String fqClassName) {
		String packageName = fqClassName.substring(0, fqClassName.lastIndexOf("."));
		String className = fqClassName.substring(fqClassName.lastIndexOf(".") + 1);
		
		JavaClassSource javaClass = Roaster.create(JavaClassSource.class)
			.setPackage(packageName)
			.setName(className);
		
		javaClass.addAnnotation(RemoteResource.class)
			.setStringValue("/");
		
		javaClass.addField()
			.setName("id")
			.addAnnotation(Id.class);
		
		return javaClass;
	}
}