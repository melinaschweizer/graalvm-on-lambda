package com.graalvmonlambda.product;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
//import com.ilmlf.delivery.api.handlers.service.SlotService;
//import com.ilmlf.delivery.api.handlers.util.ApiUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A Lambda handler for CreateSlot API Call.
 */
public class CreateSlots implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>{ //APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;
  //private static final Logger logger = LogManager.getLogger(CreateSlots.class);

  /**
   * Constructor called by AWS Lambda.
   */
  public CreateSlots() {
    this.slotService = new SlotService();
    //logger.info("CreateSlots empty constructor, called by AWS Lambda");
  }

  /**
   * Constructor for unit testing. Allow test code to inject mocked SlotService.
   *
   * @param slotService the mocked SlotService instance
   */
  public CreateSlots(SlotService slotService) {
    this.slotService = slotService;
    //logger.info("CreateSlots constructor for unit testing, allowing injection of mock SlotService");
  }

  /**
   * Handle create-slots POST via Api Gateway.
   * pathParameters expected: {farm-id=Integer}
   * <pre>
   * POST Body expected: {
      slots: [
          {
              numDeliveries: "2",
              from: "2020-01-01T10:00:00",
              to: "2020-01-01T10:00:00"
          }
      ]
   }
   * </pre>
   *
   * @return 200: success<br/>
   *        4xx: if request doesn't come from authenticated client app<br/>
   *        5xx: if slot can't be persisted
   */
  @Override
  public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
  //public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String returnVal = "";
    Integer httpStatus = 200;
    List<Slot> slotList = new ArrayList<>();
    context.getLogger().log("received request with body " + input.getBody());
    context.getLogger().log("received request with input " + input.toString());
    context.getLogger().log("is connection valid" + this.slotService.isConnectionValid());
    
    try {
    	this.slotService.refreshDbConnection();
    }catch (Exception e) {
    	context.getLogger().log("error while connecting to db: "+ e.getMessage());
    }
    
    
    try {
      String farmIdStr = "";//input.getPathParameters().get("farm-id");
      String body = "{"
      		+ "      slots: ["
      		+ "          {"
      		+ "              numDeliveries: \"2\","
      		+ "              from: \"2020-01-01T10:00:00\","
      		+ "              to: \"2020-01-01T10:00:00\""
      		+ "          }"
      		+ "      ]"
      		+ "   }"; 
    		  
    		  //input.getBody();
      slotList = parseAndCreateSlotList(body, farmIdStr, context);

    } catch (Exception e) {
      returnVal = "The data received is incomplete or invalid";
      httpStatus = 400;
      context.getLogger().log(e.getMessage());//.error(e.getMessage(), e);
    }

    if (!slotList.isEmpty()) {
      try {
        int rowsUpdated = slotService.insertSlotList(slotList);

        if (rowsUpdated == 0) {
          returnVal = "There was an error and the data could not be saved";
          httpStatus = 500;
          context.getLogger().log("there was an unknown error");
        } else {
          returnVal = "Slot data (" + rowsUpdated + ") was saved successfully";
          context.getLogger().log("returning success");
        }

      } catch (Exception e) {
        returnVal = "Error encountered while inserting the slot list";
        httpStatus = 500;
        context.getLogger().log(e.getMessage());//.error(e.getMessage(), e);
      }
    }
    context.getLogger().log("returning success 1");
    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }

  /**
   * Parses the POST body and creates a list of Slot objects from it.
   *
   * @param body the Json formatted body of the request
   * @param farmIdStr the farmId as a String
   *
   * @return the List of Slot objects
   */
  public List<Slot> parseAndCreateSlotList(String body, String farmIdStr, Context context) {
    JSONObject bodyJson = new JSONObject(body);
    JSONArray slots = bodyJson.getJSONArray("slots");
    Stream<Object> slotsStream = StreamSupport.stream(slots.spliterator(), false);

    List<Slot> slotList = slotsStream
        .map(slot -> (Slot) parseAndCreateSlot((JSONObject) slot, farmIdStr))
        .collect(Collectors.toList());

    context.getLogger().log("slotlist size "+ slotList.size());
    return slotList;
  }

  /**
   * Parses Json data and appends farmId to create a Slot object. <br/>
   * If any errors/exceptions encountered, will throw a RuntimeException
   * (in order for the function to be called via a lambda)
   *
   * @param slotJson the Json format slot data
   * @param farmIdStr the farmId as a String
   *
   * @return the Slot object
   */
  public Slot parseAndCreateSlot(JSONObject slotJson, String farmIdStr) {
    String fromStr = slotJson.getString("from");
    LocalDateTime slotFrom = Slot.getLocalDateTimeFromIso(fromStr);

    Slot slot = Slot.builder()
        .farmId(2)	//Integer.parseInt(farmIdStr))
        .from(slotFrom)
        .to(Slot.getLocalDateTimeFromIso(slotJson.getString("to")))
        .availDeliveries(slotJson.getInt("numDeliveries"))
        .bookedDeliveries(0)
        .deliveryDate(slotFrom.toLocalDate())
        .build();

    return slot;
  }
}
