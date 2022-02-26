package com.github.marciovmartins.problem.spring.web.expanded

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import kotlin.reflect.full.cast
import org.springframework.data.rest.core.RepositoryConstraintViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.NativeWebRequest
import org.zalando.problem.Problem
import org.zalando.problem.ProblemBuilder
import org.zalando.problem.spring.web.advice.ProblemHandling
import org.zalando.problem.violations.Violation

@ControllerAdvice
class ProblemExceptionHandler : ProblemHandling {
    override fun isCausalChainsEnabled() = false

    override fun handleMessageNotReadableException(
        exception: HttpMessageNotReadableException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> {
        val problemBuilder = Problem.builder()
            .withTitle("Constraint Violation")
            .withStatus(defaultConstraintViolationStatus())
        when (val cause = exception.cause) {
            is MissingKotlinParameterException -> processMissingKotlinParameter(problemBuilder, cause)
            is InvalidFormatException -> processInvalidFormatException(problemBuilder, cause)
            is JsonMappingException -> processingJsonMappingException(problemBuilder, cause)
            else -> problemBuilder.withDetail(exception.message!!)
        }
        return create(problemBuilder.build(), request)
    }

    @ExceptionHandler
    fun handleRepositoryConstraintViolationException(
        exception: RepositoryConstraintViolationException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> {
        val violations = exception.errors.allErrors
            .map { FieldError::class.cast(it) }
            .map { Violation(it.field, it.defaultMessage!!) }
        val problem = Problem.builder()
            .withTitle("Constraint Violation")
            .withStatus(defaultConstraintViolationStatus())
            .with("violations", violations)
            .build()
        return create(problem, request)
    }

    private fun processMissingKotlinParameter(problemBuilder: ProblemBuilder, cause: MissingKotlinParameterException) {
        problemBuilder.with("violations", setOf(Violation(cause.path.mapFieldPath(), "cannot be null")))
    }

    private fun processInvalidFormatException(problemBuilder: ProblemBuilder, cause: InvalidFormatException) {
        val fieldPath = cause.path.mapFieldPath()
        if (cause.message != null && cause.message!!.contains("UUID has to be represented by standard 36-char representation")) {
            problemBuilder.with("violations", setOf(Violation(fieldPath, "Invalid uuid format")))
        } else {
            problemBuilder.with("violations", setOf(Violation(fieldPath, extractMessage(cause))))
        }
    }

    private fun processingJsonMappingException(problemBuilder: ProblemBuilder, cause: JsonMappingException) {
        problemBuilder.with("violations", setOf(Violation(cause.path.mapFieldPath(), cause.cause!!.message!!)))
    }

    private fun extractMessage(cause: InvalidFormatException) = if (cause.cause == null) {
        cause.message
    } else {
        cause.cause!!.message
    }!!
}

private fun Collection<JsonMappingException.Reference>.mapFieldPath() =
    this.joinToString(separator = "") { mapPath(it) }.substring(1)

private fun mapPath(it: JsonMappingException.Reference): String = when (it.from) {
    is Collection<*> -> "[]"
    else -> ".${it.fieldName}"
}