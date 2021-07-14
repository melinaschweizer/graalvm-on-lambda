package com.graalvmonlambda.product;

//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

/**
 * Utility class for API Gateway handlers.
 */
public abstract class ApiUtil {

  /**
   * Generate returned data for API Gateway.
   *
   * @param httpStatus Returned HTTP code.
   * @param message Message to be returned.
   * @return a response to API Gateway
   */
  public static APIGatewayV2HTTPResponse generateReturnData(Integer httpStatus, String message) {
    //APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
	//    response.setStatusCode(httpStatus);
	//    response.setBody(message + " ok");
	  //return response;
	
	return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(httpStatus)
            .withBody(message)
            .build();    
  }
}
