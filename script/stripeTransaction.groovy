import java.util.HashMap
import java.util.Map

import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.Charge
import com.stripe.net.RequestOptions
import com.stripe.model.Token

def logger = ec.getLogger()



Stripe.apiKey = ""

Map<String, Object> tokenParams = new HashMap<String, Object>()
Map<String, Object> cardParams = new HashMap<String, Object>()
cardParams.put("number", "4242424242424242")
cardParams.put("exp_month", 7)
cardParams.put("exp_year", 2020)
cardParams.put("cvc", "314")
tokenParams.put("card", cardParams)

def tok = Token.create(tokenParams)

Map<String, Object> chargeMap = new HashMap<String, Object>()
chargeMap.put("amount", 100)
chargeMap.put("currency", "usd")
chargeMap.put("description", "from moqui/groovy/stripelib")
chargeMap.put("source", tok.id)

try {
    def charge = Charge.create(chargeMap)

    logger.info("\n#\n#\n#")
    logger.info(chargeMap.toString())
    logger.info("\n#\n#\n#")
} catch (StripeException e) {
    logger.info("\n#\n#\n#")
    logger.error(e.toString())
    logger.info("\n#\n#\n#")
}