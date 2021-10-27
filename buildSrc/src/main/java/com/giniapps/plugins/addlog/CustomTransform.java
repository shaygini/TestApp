package com.giniapps.plugins.addlog;


import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;


import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;


class CustomTransform extends Transform {


    @Override
    public String getName() {
        return "CustomTransformName";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        System.err.println("getInputTypes");
        HashSet set = new HashSet<QualifiedContent.ContentType>();
        set.add(QualifiedContent.DefaultContentType.CLASSES);
        return set;
    }

    @Override
    public Set<QualifiedContent.ContentType> getOutputTypes() {
        System.err.println("getOutputTypes");
        HashSet set = new HashSet<QualifiedContent.ContentType>();
        set.add(QualifiedContent.DefaultContentType.CLASSES);
        return set;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        System.err.println("getScopes");
        HashSet set = new HashSet<QualifiedContent.Scope>();
        set.add(QualifiedContent.Scope.PROJECT);

        return set;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {
        System.err.println("transform");
        Set<String> classNames = getClassNames(transformInvocation.getInputs());
        ClassPool classPool = createClassPoll(transformInvocation.getInputs(), transformInvocation.getReferencedInputs());

        Set<CtClass> ctClasses = getCtClasses(classPool, classNames);

        processClasses(ctClasses);

        transformInvocation.getOutputProvider().deleteAll();
        String outputPath = getOutputPath(transformInvocation.getOutputProvider());
        saveClasses(outputPath, ctClasses);
        System.err.println("finish transform");

    }

    Set<String> getClassNames(Collection<TransformInput> inputs) {
        Set<String> classNames = new HashSet<>();

        for (TransformInput input : inputs) {
            classNames.addAll(getDirectoryInputs(input.getDirectoryInputs()));
            classNames.addAll(getJarInputs(input.getJarInputs()));
        }
        return classNames;
    }

    // Todo: need to check what happsns in incremental build and use the getChangedFiles method
    Set<String> getDirectoryInputs(Collection<DirectoryInput> directoryInputs) {
        Set<String> classNames = new HashSet<>();
        for (DirectoryInput input : directoryInputs) {
            try {
                classNames.addAll(processDirectoryInput(input));
            } catch (IOException e) {
                throw new GradleException(e.getMessage());
            }
        }
        return classNames;
    }

    Set<String> processDirectoryInput(DirectoryInput input) throws IOException {
        String dirPath = input.getFile().getAbsolutePath();

        return Files.walk(input.getFile().toPath())
                .map(file -> file.toAbsolutePath().toString())
                .filter(path -> path.endsWith(SDKConstants.DOT_CLASS))
                .map(path -> path.substring(dirPath.length() + 1,
                        path.length() - SDKConstants.DOT_CLASS.length()))
                .map(path -> path.replaceAll(File.separator, "."))
                .collect(Collectors.toSet());
    }

    Set<String> getJarInputs(Collection<JarInput> jarInputs) {
        return jarInputs.stream().map(QualifiedContent::getFile)
                .map(this::toJar)
                .map(JarFile::entries)
                .flatMap(this::toZipEntryStream)
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(SDKConstants.DOT_CLASS))
                .map(ZipEntry::getName)
                .map(name -> name.substring(0, name.length() - SDKConstants.DOT_CLASS.length()))
                .map(name -> name.replaceAll(File.separator, "."))
                .collect(Collectors.toSet());
    }

    ClassPool createClassPoll(Collection<TransformInput> inputs, Collection<TransformInput> referencedInput) {
        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));

        Stream.concat(inputs.stream(), referencedInput.stream())
                .flatMap(input -> Stream.concat(input.getDirectoryInputs().stream(),
                        input.getJarInputs().stream()))
                .map(input -> input.getFile().getAbsolutePath())
                .forEach(entry -> {
                    try {
                        classPool.appendClassPath(entry);
                    } catch (NotFoundException e) {
                        throw new GradleException(e.getMessage());
                    }
                });

        return classPool;
    }

    JarFile toJar(File file) {
        try {
            return new JarFile(file);
        } catch (IOException e) {
            return null;
        }
    }

    Stream<ZipEntry> toZipEntryStream(Enumeration<JarEntry> enumeration) {
        return enumerationAsStream(enumeration)
                .map(item -> new ZipEntry(item));
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }

                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }

    public String getOutputPath(TransformOutputProvider transformOutputProvider) {
        return transformOutputProvider.getContentLocation(
                "classes",
                getOutputTypes(),
                getScopes(),
                Format.DIRECTORY
        ).getAbsolutePath();
    }

    public void saveClasses(String outputPath, Set<CtClass> ctClasses) {
        for (CtClass ctClass : ctClasses) {
            try {
                ctClass.writeFile(outputPath);
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Set<CtClass> getCtClasses(ClassPool classPool, Set<String> classNames) {
        Set<CtClass> ctClasses = new HashSet<>();
        for (String className : classNames) {
            try {
                ctClasses.add(classPool.get(className));
            } catch (NotFoundException e) {
                // noting
            }

        }

        return ctClasses;
    }

    void processClasses(Set<CtClass> ctClasses) {
        for (CtClass ctClass : ctClasses) {
           // addLogMethod(ctClass);
           // addLogInGoogleAnalytics(ctClass);
        }
    }

    void addLogMethod(CtClass ctClass) {
        String body = "System.out.println(\"Added later\" + " + "\"" + ctClass.getName() + "\" + " + "this.getClass().getSimpleName());";
        CtConstructor[] constructors = ctClass.getConstructors();
        if (constructors == null) return;
        for (CtConstructor constructor : constructors) {
            // need to replace with addCodeIfNeeded
            addCode(constructor, body);
        }
    }

    void addLogInGoogleAnalytics(CtClass ctClass) {
        if(ctClass.getName().equals("com.google.firebase.analytics.FirebaseAnalytics")) {
            String body = "System.out.println(\"Added later\" + " + "\"" + ctClass.getName() + "\" + " + "this.getClass().getSimpleName());";
            CtMethod ctMethod = null;
            try {
                CtMethod[] methods =  ctClass.getMethods();
                ctMethod = ctClass.getMethod("logEvent", "(Ljava/lang/String;Landroid/os/Bundle;)V");
                ctMethod.insertAfter(body);
            } catch (NotFoundException | CannotCompileException e) {
                e.printStackTrace();
            }

        }

    }

    void addCode(CtConstructor constructor, String body) {
        try {
            constructor.insertBeforeBody(body);
        } catch (CannotCompileException e) {
            System.err.println(e.getMessage());
        }
    }

}