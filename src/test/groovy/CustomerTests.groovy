import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.screen.ScreenTest
import org.moqui.screen.ScreenTest.ScreenTestRender
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import com.stripe.Stripe
import com.stripe.model.Customer

class CustomerTests extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(CustomerTests.class)

    @Shared ExecutionContext ec

    def setupSpec() {
        println("START_SETUP_SPEC")
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        println("START_CLEANUP_SPEC")
        ec.destroy()
    }

    def setup() {
        println("START_SETUP")
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("john.doe", "moqui")

        def pgs = ec.entity.find("Stripe.PaymentGatewayStripe").condition("paymentGatewayConfigId", "StripeDemo").one()
        Stripe.apiKey = pgs.secretKey
    }

    def cleanup() {
        println("START_CLEANUP")
        ec.artifactExecution.enableAuthz()
        ec.user.logoutUser()
    }

//    def "Stripe customer is created when a credit card is created (via SECA)"() {
//        given:
//        def paymentMethod = ec.service.sync().name("create#mantle.account.method.PaymentMethod").call()
//
//        when:
//        def creditCard = ec.service.sync().name("mantle.account.PaymentMethodServices.create#CreditCard")
//                .parameters([
//                        paymentMethodId:paymentMethod.paymentMethodId,
//                        cardNumber:"4242424242424242",
//                        expireDate:"12/2020",
//                        cardSecurityCode:"123"
//                ]).call()
//
//        then:
//        def updatedPaymentMethod = ec.entity.find("mantle.account.method.PaymentMethod").condition('paymentMethodId', paymentMethod.paymentMethodId).one()
//        updatedPaymentMethod.gatewayCimId != null
//
//        def customer = Customer.retrieve(updatedPaymentMethod.gatewayCimId)
//        customer?.error == null
//        customer.id == updatedPaymentMethod.gatewayCimId
//
//        cleanup:
//        ec.service.sync().name("delete#mantle.account.method.CreditCard").parameters([paymentMethodId: creditCard.paymentMethodId]).call()
//        ec.service.sync().name("delete#mantle.account.method.PaymentMethod").parameters([paymentMethodId: paymentMethod.paymentMethodId]).call()
//    }
}