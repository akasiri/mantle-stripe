package com.stripe

import com.stripe.Stripe
import com.stripe.model.Token
import com.stripe.exception.StripeException

import org.moqui.context.ExecutionContext

class TokenServices {
    static Map generateToken(ExecutionContext ec) {
        def secretKey = ec.context.secretKey
        def creditCardInfo = ec.context.creditCardInfo

        Stripe.apiKey = secretKey

        def tokenParams = ['card':creditCardInfo]

        def responseMap = [:]

        try {
            def token = Token.create(tokenParams)

            responseMap.token = token
            responseMap.errorInfo = ['responseCode':'1'] // '1' = success
        }
        catch (StripeException e) {
            responseMap.errorInfo = ['responseCode':'','reasonCode':e.getCode(),'reasonMessage':e.getMessage()]
            if (e.getCode() == "incorrect_number") responseMap.responseCode = "6"
            else if (e.getCode() == "invalid_expiry_year" || e.getCode() == "invalid_expiry_month") responseMap.responseCode = "7"
            else responseMap.responseCode = "3"
        }
        catch (Exception e) {
            responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage':e.getMessage()]
        }

        return ['responseMap':responseMap]
    }
}