package com.stripe

import com.stripe.Stripe
import com.stripe.model.Charge
import com.stripe.model.Refund
import com.stripe.exception.StripeException
import com.stripe.exception.CardException
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCaptureParams;

import org.moqui.context.ExecutionContext

class TransactionServices {
    static Map sendAuthorizeRequest (ExecutionContext ec) {
        def secretKey = ec.context.secretKey
        def creditCardInfo = ec.context.creditCardInfo
        def transactionInfo = ec.context.transactionInfo
        transactionInfo.amount = transactionInfo.amount.toInteger() * 100 // dollars to cents needed because stripe records USD amounts by the smallest division (cents)
        Stripe.apiKey = secretKey
        def responseMap = [:]

        if (creditCardInfo.paymentRefNum) {
            PaymentIntent intent;
            try {
                PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
                        .setAmount(transactionInfo.amount)
                        .setCurrency("INR")
                        .setConfirm(true)
                        .addPaymentMethodType("card")
                        .setPaymentMethod(creditCardInfo.paymentRefNum)
                        .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)// TODO Use transactionInfo.capture to define capture
                        .build();
                        // TODO Understand and use .setConfirm(true)
                        // TODO Understand and use .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                        // TODO Use it for separate capturing
                intent = PaymentIntent.create(createParams);
                // TODO Find a better way using intent object
                responseMap.amount = transactionInfo.amount;
                // TODO Handle case if authorisation require multi step authentication (like 3D Secure authentication)
                // intent = PaymentIntent.retrieve(confirmRequest.getPaymentIntentId());
                // intent = intent.confirm();

                // Note that if your API version is before 2019-02-11, 'requires_action'
                // appears as 'requires_source_action'.
                if (intent.getStatus().equals("requires_action")
                        && intent.getNextAction().getType().equals("use_stripe_sdk")) {
                    // TODO
                    // responseData.put("requires_action", true);
                    // responseData.put("payment_intent_client_secret", intent.getClientSecret());
                    responseMap.errorInfo = ['responseCode':'1','requires_action': true, 'payment_intent_client_secret': intent.getClientSecret()]
                } else if (intent.getStatus().equals("requires_confirmation")) {
                    // TODO
                    // responseData.put("requires_action", true);
                    // responseData.put("payment_intent_client_secret", intent.getClientSecret());
                    responseMap.errorInfo = ['responseCode':'1','requires_confirmation': true, 'payment_intent_client_secret': intent.getClientSecret()]
                } else if (intent.getStatus().equals("succeeded")) {
                    responseMap.errorInfo = ['responseCode':'1', 'payment_intent_client_secret': intent.getClientSecret()] // '1' = success
                } else {
                    // invalid status
                    responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage': 'Invalid status']
                }
            } catch (Exception e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage':e.getMessage(),'exception':e]
            }
        } else {
            def tokenResponse = TokenServices.generateToken(ec).responseMap
            if (!tokenResponse.token) return ['responseMap':tokenResponse] // passing the error info back up to caller

            transactionInfo.source = tokenResponse.token.id

            try {
                def charge = Charge.create(transactionInfo)

                responseMap.charge = charge
                responseMap.errorInfo = ['responseCode':'1'] // '1' = success

            } catch (CardException e) {
                responseMap.errorInfo = ['responseCode':'2','reasonCode':e.getCode(),'reasonMessage':e.getMessage(),'exception':e]
            } catch (StripeException e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':e.getCode(),'reasonMessage':e.getMessage(),'exception':e]
            } catch (Exception e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage':e.getMessage(),'exception':e]
            }
        }

        return ['responseMap':responseMap]
    }

    static Map sendCaptureRequest (ExecutionContext ec) {
        def secretKey = ec.context.secretKey
        def chargeId = ec.context.chargeId
        def amount = ec.context.amount
        def referenceNum = ec.context.referenceNum;
        def intentSecret = ec.context.intentSecret;

        amount = amount.toInteger() * 100 // dollars to cents needed because stripe records USD amounts by the smallest division (cents)

        Stripe.apiKey = secretKey

        def responseMap = [:]

        if(referenceNum) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(intentSecret);

                PaymentIntentCaptureParams params =
                        PaymentIntentCaptureParams.builder()
                                .setAmountToCapture(amount)
                                .build();

                intent.capture(params);
                responseMap.errorInfo = ['responseCode':'1'] // '1' = success

            } catch (StripeException e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':e.getCode(),'reasonMessage':e.getMessage(),'exception':e]
            } catch (Exception e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage':e.getMessage(),'exception':e]
            }

        } else {
            try {
                def charge = Charge.retrieve(chargeId)
                if (amount != null) charge.capture(['amount':amount])
                else charge.capture()

                responseMap.charge = charge
                responseMap.errorInfo = ['responseCode':'1'] // '1' = success

            } catch (StripeException e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':e.getCode(),'reasonMessage':e.getMessage(),'exception':e]
            } catch (Exception e) {
                responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage':e.getMessage(),'exception':e]
            }
        }
        return ['responseMap':responseMap]
    }

    static Map sendRefundRequest (ExecutionContext ec) {
        def secretKey = ec.context.secretKey
        def chargeId = ec.context.chargeId
        def amount = ec.context.amount

        amount = amount.toInteger() * 100 // dollars to cents needed because stripe records USD amounts by the smallest division (cents)

        Stripe.apiKey = secretKey

        def responseMap = [:]

        try {
            def refund
            if (amount != null) refund = Refund.create(['charge':chargeId,'amount':amount])
            else refund = Refund.create(['charge':chargeId])

            responseMap.refund = refund
            responseMap.errorInfo = ['responseCode':'1'] // '1' = success

        } catch (StripeException e) {
            responseMap.errorInfo = ['responseCode':'3','reasonCode':e.getCode(),'reasonMessage':e.getMessage(),'exception':e]
        } catch (Exception e) {
            responseMap.errorInfo = ['responseCode':'3','reasonCode':'','reasonMessage':e.getMessage(),'exception':e]
        }

        return ['responseMap':responseMap]
    }
}