/*
 * Copyright 2016 Black Pepper Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.blackpepper.bowman;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.ReflectionUtils;

import uk.co.blackpepper.bowman.annotation.LinkedResource;
import uk.co.blackpepper.bowman.annotation.RemoteResource;
import uk.co.blackpepper.bowman.annotation.ResourceId;

import static uk.co.blackpepper.bowman.HalSupport.toLinkName;

final class ReflectionSupport {

	private static final Class<ResourceId> ID_ACCESSOR_ANNOTATION = ResourceId.class;
	
	private ReflectionSupport() {
	}
	
	public static URI getId(Object object) {
		Method accessor = getIdAccessor(object.getClass());
		return (URI) ReflectionUtils.invokeMethod(accessor, object);
	}

	public static void setId(Object value, URI uri) {
		Field idField = getIdField(value.getClass());
		idField.setAccessible(true);
		ReflectionUtils.setField(idField, value, uri);
	}
	
	public static String getResourcePath(final Class<?> type) {
		return type.getAnnotation(RemoteResource.class).value();
	}
	
	public static Map<String, List<URI>> getAssociationsURIs(Object value) {
		Map<String, List<URI>> associations = new HashMap<>();
		
		List<Method> linkedResources = getMethodsAnnotatedWith(value.getClass(), LinkedResource.class);
		if (!linkedResources.isEmpty()) {
			for (Method linkedResource : linkedResources) {
				List<URI> values = new ArrayList<>();
				try {
					if (Collection.class.isAssignableFrom(linkedResource.getReturnType())) {
						Collection resources = (Collection) linkedResource.invoke(value);
						for (Object resource : resources) {
							addIfNotNull(resource, values);
						}
					}
					else {
						addIfNotNull(linkedResource.invoke(value), values);
					}
				}
				catch (IllegalAccessException | InvocationTargetException e) {
					return Collections.emptyMap();
				}
				associations.put(getRelName(linkedResource), values);
			}
		}
		
		return associations;
	}
	
	public static String getRelName(Method method) {
		String linkName = method.getAnnotation(LinkedResource.class).rel();
		
		if ("".equals(linkName)) {
			linkName = toLinkName(method.getName());
		}
		
		return linkName;
	}
	
	public static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
		final List<Method> methods = new ArrayList<>();
		Class<?> clazz = type;
		while (clazz != Object.class) {
			final List<Method> allMethods = new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods()));
			for (final Method method : allMethods) {
				if (method.isAnnotationPresent(annotation)) {
					methods.add(method);
				}
			}
			// move to the upper class in the hierarchy in search for more methods
			clazz = clazz.getSuperclass();
		}
		return methods;
	}
	
	private static void addIfNotNull(Object object, List<URI> to) {
		URI id = getId(object);
		if (id != null) {
			to.add(id);
		}
	}
	
	private static Method getIdAccessor(Class<?> clazz) {
		for (Method method : ReflectionUtils.getAllDeclaredMethods(clazz)) {
			if (method.getAnnotation(ID_ACCESSOR_ANNOTATION) != null) {
				return method;
			}
		}
		
		throw new IllegalArgumentException(String.format("No @%s found for %s",
			ID_ACCESSOR_ANNOTATION.getSimpleName(), clazz.getName()));
	}

	private static Field getIdField(Class<?> clazz) {
		Method idAccessor = getIdAccessor(clazz);
		return ReflectionUtils.findField(clazz, HalSupport.toLinkName(idAccessor.getName()));
	}
}
