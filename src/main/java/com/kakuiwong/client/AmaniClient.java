package com.kakuiwong.client;

import com.kakuiwong.client.annotation.AmaniReference;
import com.kakuiwong.client.annotation.EnableAmaniRpcClient;
import com.kakuiwong.client.handler.AmaniProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

/**
 * @author gaoyang
 * @email 785175323@qq.com
 */
@Component
public class AmaniClient implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.addIncludeFilter(new AnnotationTypeFilter(AmaniReference.class));
        String basePackage = getPackage(registry);
        Set<BeanDefinition> candidateComponents = scanner
                .findCandidateComponents(basePackage.equals("") ? getBasePackage(registry) : basePackage);
        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition) {
                AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                BeanDefinitionHolder holder = createBeanDefinition(annotationMetadata);
                BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (beanDefinition.getMetadata().isInterface()
                            && beanDefinition.getMetadata().getInterfaceNames().length == 1
                            && Annotation.class.getName().equals(beanDefinition.getMetadata().getInterfaceNames()[0])) {
                        try {
                            Class<?> target = Class.forName(beanDefinition.getMetadata().getClassName());
                            return !target.isAnnotation();
                        } catch (Exception ex) {
                        }
                    }
                    return true;
                }
                return false;
            }
        };
    }

    private BeanDefinitionHolder createBeanDefinition(AnnotationMetadata annotationMetadata) {
        String className = annotationMetadata.getClassName();

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(AmaniProxy.class);
        String beanName = StringUtils.uncapitalize(className.substring(className.lastIndexOf('.') + 1));

        definition.addPropertyValue("type", className);

        return new BeanDefinitionHolder(definition.getBeanDefinition(), beanName);
    }

    private String getPackage(BeanDefinitionRegistry registry) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) registry;
        return factory.getBeansWithAnnotation(EnableAmaniRpcClient.class).entrySet().stream().map(Map.Entry::getValue).findFirst().get().
                getClass().getAnnotation(EnableAmaniRpcClient.class).basePackage();
    }

    private String getBasePackage(BeanDefinitionRegistry registry) {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) registry;
        String name = factory.getBeansWithAnnotation(EnableAmaniRpcClient.class).entrySet().stream().map(Map.Entry::getValue).findFirst().get().
                getClass().getName();
        return name.substring(0, name.indexOf('.'));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
