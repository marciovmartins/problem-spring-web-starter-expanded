package com.github.marciovmartins.problem.spring.web.expanded

import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.problem.spring.web.advice.AdviceTrait
import org.zalando.problem.spring.web.autoconfigure.ProblemAutoConfiguration

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@AutoConfigureBefore(WebMvcAutoConfiguration::class, ProblemAutoConfiguration::class)
class ProblemExtendedAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AdviceTrait::class)
    fun exceptionHandling(): AdviceTrait {
        return ProblemExceptionHandler()
    }
}