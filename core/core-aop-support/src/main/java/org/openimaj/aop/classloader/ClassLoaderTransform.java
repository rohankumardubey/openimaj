package org.openimaj.aop.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javassist.ClassPool;
import javassist.Loader;

import org.openimaj.aop.ClassTransformer;
import org.openimaj.aop.MultiTransformClassFileTransformer;

/**
 * {@link ClassLoaderTransform} provides an alternative to using a java agent to
 * perform byte-code manipulation by providing a classloader that will
 * automatically transform classes as they are loaded.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 */
public class ClassLoaderTransform {
	private static class ParentLoader extends URLClassLoader {
		public ParentLoader(URL[] paramArrayOfURL) {
			super(paramArrayOfURL);
		}

		// @Override
		// public Package getPackage(String paramString) {
		// return super.getPackage(paramString);
		// }
	}

	/**
	 * Manifest property key for specifying the actual main class when using the
	 * loading technique provided by {@link #main(String[])}.
	 */
	public static final String MAIN_CLASS = "OpenIMAJ-Main-Class";

	/**
	 * Manifest property key for specifying the {@link ClassTransformer} classes
	 * when using the loading technique provided by {@link #main(String[])}.
	 */
	public static final String TRANSFORMERS = "OpenIMAJ-Transformers";

	/**
	 * Separator character for separating multiple {@link ClassTransformer}s
	 * when using the loading technique provided by {@link #main(String[])}.
	 */
	public static final String TRANSFORMERS_SEPARATOR = ",";

	/**
	 * Run the main method of the given jar file, passing all classes as they
	 * are loaded through the transformer to modify the bytecode. The main
	 * method must be specified by the "Main-Class" manifest entry.
	 * 
	 * @param pool
	 *            the classpool to hold classes as they are loaded
	 * @param tf
	 *            the transform(s) to apply
	 * @param jarFile
	 *            the jar file to run
	 * @param args
	 *            the arguments to pass to the main method
	 * @return the classloader used for loading the classes in the jar file
	 * @throws Throwable
	 *             if an error occurs
	 */
	public static Loader run(ClassPool pool, MultiTransformClassFileTransformer tf, File jarFile, String[] args)
			throws Throwable
	{
		JarFile jar = null;

		try {
			jar = new JarFile(jarFile);
			final Manifest manifest = jar.getManifest();
			final Attributes attr = manifest.getMainAttributes();
			final String mainClass = attr.getValue("Main-Class");

			pool.appendClassPath(jarFile.getAbsolutePath());

			final ParentLoader parent = new ParentLoader(new URL[] { jarFile.toURI().toURL() });

			return run(parent, pool, tf, mainClass, args);
		} finally {
			try {
				if (jar != null)
					jar.close();
			} catch (final IOException e) {
			}
		}
	}

	/**
	 * Run the main method of the given class, transforming any classes found on
	 * the given classpath as they are loaded.
	 * 
	 * @param pool
	 *            the classpool to hold classes as they are loaded
	 * @param tf
	 *            the transform(s) to apply
	 * @param classpath
	 *            the classpath on which to search for classes
	 * @param mainClass
	 *            the main class with the <code>main(String[])</code> method
	 * @param args
	 *            the arguments to pass to the main method
	 * @return the classloader used for loading the classes in the jar file
	 * @throws Throwable
	 *             if an error occurs
	 */
	public static Loader run(ClassPool pool, MultiTransformClassFileTransformer tf, String classpath, String mainClass,
			String[] args)
			throws Throwable
	{
		final String[] paths = classpath.split(File.pathSeparator);
		final URL[] urls = new URL[paths.length];
		for (int i = 0; i < paths.length; i++)
			urls[i] = new File(paths[i]).toURI().toURL();

		final ParentLoader parent = new ParentLoader(urls);

		pool.appendPathList(classpath);

		return run(parent, pool, tf, mainClass, args);
	}

	/**
	 * Re-load the given class in a newly created {@link Loader} that is
	 * configured to apply the given transform(s), and then run the main method.
	 * 
	 * @param transformer
	 *            the transformer
	 * @param clz
	 *            the class
	 * @param args
	 *            the arguments
	 * @throws Throwable
	 *             if an error occurs
	 */
	public static void run(MultiTransformClassFileTransformer transformer, Class<?> clz, String[] args) throws Throwable {
		final ClassPool pool = new ClassPool();
		pool.appendSystemPath();

		final Loader loader = new Loader(pool);

		// skip args4j
		loader.delegateLoadingOf("org.apache.log4j.");

		loader.addTranslator(pool, transformer);

		loader.run(clz.getName(), args);
	}

	protected static Loader run(final ParentLoader parent, ClassPool pool, MultiTransformClassFileTransformer tf,
			String mainClass, String[] args)
			throws Throwable
	{
		final Loader cl = new Loader(parent, pool);
		// {
		// @Override
		// protected Package getPackage(String name) {
		// if (parent != null)
		// return parent.getPackage(name);
		// return super.getPackage(name);
		// }
		// };

		// Set the correct app name on OSX. Are there similar controls for other
		// platforms?
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", mainClass);

		// skip args4j
		cl.delegateLoadingOf("org.apache.log4j.");

		cl.addTranslator(pool, tf);

		cl.run(mainClass, args);

		return cl;
	}

	/**
	 * If the the given class has not already been loaded in by {@link Loader},
	 * load it in a newly created {@link Loader} that is configured to apply the
	 * given transform(s), run the main method and return true. If the class's
	 * classload is a {@link Loader} then return false.
	 * 
	 * @param clz
	 *            the class to load, transform and run
	 * @param args
	 *            the arguments to the main method
	 * @param transformer
	 *            the first transformer
	 * @param transformers
	 *            any additional transformers
	 * @return true if the class has been reloaded; false otherwise
	 * @throws Throwable
	 *             if an error occurs.
	 */
	public static boolean
			run(Class<?> clz, String[] args, ClassTransformer transformer, ClassTransformer... transformers)
					throws Throwable
	{
		if (!(clz.getClassLoader() instanceof Loader)) {
			final MultiTransformClassFileTransformer mtf =
					new MultiTransformClassFileTransformer(transformer, transformers);

			ClassLoaderTransform.run(mtf, clz, args);

			return true;
		}

		return false;
	}

	/**
	 * Main method for an alternative in-place loading strategy. Allows a
	 * self-contained program jar to automatically run {@link ClassTransformer}s
	 * on itself.
	 * <p>
	 * Modify the assembled jar file so that the Main-Class is set as this class
	 * (<code>org.openimaj.aop.classloader.ClassLoaderTransform</code>) and add
	 * additional keys to the manifest to point at the actual class main (
	 * {@link #MAIN_CLASS}) and the transformer classes ({@link #TRANSFORMERS},
	 * separated by {@link #TRANSFORMERS_SEPARATOR}).
	 * <p>
	 * The specified transformer classes must all be instances of
	 * {@link ClassTransformer} and also have a no-args constructor.
	 * 
	 * @param args
	 *            the arguments to pass to the program being run
	 * @throws Throwable
	 *             if an error occurs
	 */
	public static void main(String[] args) throws Throwable {
		final Class<ClassLoaderTransform> clazz = ClassLoaderTransform.class;
		final String className = clazz.getSimpleName() + ".class";
		final String classPath = clazz.getResource(className).toString();
		if (!classPath.startsWith("jar")) {
			// Class not from JAR
			return;
		}
		final String manifestPath = classPath.substring(0,
				classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
		final Manifest manifest = new Manifest(new URL(manifestPath).openStream());
		final Attributes attr = manifest.getMainAttributes();

		final String mainClass = attr.getValue(MAIN_CLASS);
		final String[] tfs = attr.getValue(TRANSFORMERS).split(TRANSFORMERS_SEPARATOR);
		final ClassTransformer[] cts = new ClassTransformer[tfs.length];
		for (int i = 0; i < tfs.length; i++)
			cts[i] = (ClassTransformer) Class.forName(tfs[i]).newInstance();

		final ClassPool cp = ClassPool.getDefault();

		run(null,
				cp,
				new MultiTransformClassFileTransformer(cts[0], Arrays.copyOfRange(cts, 1, cts.length)),
				mainClass,
				args);
	}
}