
package com.revature.resources;



import static com.revature.utils.LogUtil.logger;



import java.io.IOException;

import java.net.URI;

import java.util.List;

import java.util.Map;

import java.util.Set;



import javax.persistence.ParameterMode;

import javax.persistence.StoredProcedureQuery;

import javax.ws.rs.Consumes;

import javax.ws.rs.DefaultValue;

import javax.ws.rs.GET;

import javax.ws.rs.HeaderParam;

import javax.ws.rs.POST;

import javax.ws.rs.PUT;

import javax.ws.rs.Path;

import javax.ws.rs.PathParam;

import javax.ws.rs.Produces;

import javax.ws.rs.QueryParam;

import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;

import javax.ws.rs.core.Response.Status;



import org.hibernate.HibernateException;

import org.hibernate.Session;

import org.hibernate.Transaction;



import com.revature.dao.AssociateDaoHibernate;

import com.revature.model.AssociateInfo;

import com.revature.model.ClientMappedJSON;

import com.revature.request.model.AssociateFromClient;

import com.revature.request.model.AssociateUserModel;

import com.revature.request.model.CreateAssociateModel;

import com.revature.services.AssociateService;

import com.revature.services.JWTService;

import com.revature.services.UserService;

import com.revature.utils.HibernateUtil;

import com.revature.utils.LogUtil;



import io.jsonwebtoken.Claims;

import io.swagger.annotations.Api;

import io.swagger.annotations.ApiOperation;

import io.swagger.annotations.ApiParam;



@Path("/associates")

@Api(value = "associates")

@Consumes(MediaType.APPLICATION_JSON)

@Produces(MediaType.APPLICATION_JSON)

public class AssociateResource {



	private UserService uservice;

	private AssociateService service = new AssociateService();

	private JWTService jService = new JWTService();

	//Constructor for creating resources
	 public AssociateResource() {

	        this.uservice = new UserService();

	    }

	/**

	 * Gets a list of all the associates, optionally filtered by a batch id. If an

	 * associate has no marketing status or curriculum, replaces them with blanks.

	 * If associate has no client, replaces it with "None".

	 *

	 * The different user types and their ID's are Admin: 1 Trainer: 2

	 * Sales/Delivery 3 Staging Manager 4 Associate 5

	 * 

	 * @return A Response object with a list of TfAssociate objects.

	 * @throws IOException

	 * @throws HibernateException

	 */

	@GET

	@ApiOperation(value = "Return all associates", notes = "Gets a set of all the associates, optionally filtered by a batch id. If an associate has no marketing status or\r\n"

			+ " curriculum, replaces them with blanks. If associate has no client, replaces\r\n"

			+ " it with \"None\".", response = AssociateInfo.class, responseContainer = "Set")

	public Response getAllAssociates(@HeaderParam("Authorization") String token) 

	{

		Status status = null;

		Set<AssociateInfo> associates = null;

		Claims payload = JWTService.processToken(token);



		if (payload == null || payload.getId().equals("5")) 

		{

			status = Status.UNAUTHORIZED;

		} 

		

		else 

		{

			associates = service.getAllAssociates();

			status = associates == null || associates.isEmpty() ? Status.NO_CONTENT : Status.OK;

		}

		

		return Response.status(status).entity(associates).build();

	}



	@PUT

	@ApiOperation(value = "Batch update associates", notes = "Updates the maretking status and/or the client of one or more associates")

	public Response updateAssociates(@HeaderParam("Authorization") String token,

			@DefaultValue("0") @ApiParam(value = "marketing status id") @QueryParam("marketingStatusId") Integer marketingStatusId,

			@DefaultValue("0") @ApiParam(value = "client id") @QueryParam("clientId") Integer clientId,

			List<Integer> ids) 

	{

		Status status = null;

		Claims payload = JWTService.processToken(token);



		if (payload == null || !payload.getId().equals("1")) 

		{

			status = Status.UNAUTHORIZED;

		} 

		

		else 

		{

			// marketing status & client id are given as query parameters, ids sent in body

			service.updateAssociates(ids, marketingStatusId, clientId);

		}

		

		return Response.ok().build();

	}



	 @POST

	    @Consumes("application/json")

	    @ApiOperation(value="Creates new Associate", notes = "Takes username, password, fname and lname to create new user")

	    public Response createNewAssociate(CreateAssociateModel newAssociate){

	        LogUtil.logger.info("createAssociate got hit");

	        LogUtil.logger.info(newAssociate);

//	        SuccessOrFailMessage msg = service.createNewAssociate(newAssociate);

//	        if (msg.getStatus()) {

//	            int userId = msg.getNewId();

//	            URI location = URI.create("/user/"+userId);

//	            return Response.created(location).build();

//	        } else {

//	            return Response.serverError().build();

//	        }

	        uservice.createNewAssociate(newAssociate);

	        return Response.created(URI.create("/testingURIcreate")).build();

	    }

	

	@GET

	@ApiOperation(value = "Return an associate", notes = "Returns information about a specific associate.", response = AssociateInfo.class)

	@Path("/{associateid}")

	public Response getAssociate(@ApiParam(value = "An associate id.") @PathParam("associateid") int associateid, @HeaderParam("Authorization") String token) 

	{

		Status status = null;

		Claims payload = JWTService.processToken(token);

		AssociateInfo associateinfo = null;



		if (payload == null || payload.getId().equals("5")) 

		{

			status = Status.UNAUTHORIZED;

		} 

		

		else 

		{

			associateinfo = service.getAssociate(associateid);

			status = associateinfo == null ? Status.NO_CONTENT : Status.OK;

		}

		

		return Response.status(status).entity(associateinfo).build();

	}



	@GET

	@ApiOperation(value = "Return an associate", notes = "Returns information about a specific associate.")

	@Path("/mapped/{statusId}")

	public Response getMappedInfo(@PathParam("statusId") int statusId, @HeaderParam("Authorization") String token) 

	{

		Status status = null;

		Claims payload = JWTService.processToken(token);

		Map<Integer, ClientMappedJSON> mappedStats = null;



		if (payload == null || !payload.getId().equals("1")) 

		{

			status = Status.UNAUTHORIZED;

			return Response.status(status).build();

		} 

		

		else 

		{

			mappedStats = service.getMappedInfo(statusId);

			if (mappedStats.isEmpty())

				return Response.status(500).build();

		}

		

		return Response.ok(mappedStats).build();

	}



	 @GET

	 @Path("unmapped/{statusId}")

	 public Response getUnmappedInfo(@PathParam("statusId") int statusId) {

	 return Response.ok(service.getUnmappedInfo(statusId)).build();

	 }



	@PUT

	@ApiOperation(value = "updates associate values", notes = "The method updates the marketing status or client of a given associate by their id.")

	@Path("/{associateId}")

	public Response updateAssociate(@PathParam("associateId") Integer id, AssociateFromClient afc, @HeaderParam("Authorization") String token) 

	{

		Status status = null;

		Claims payload = JWTService.processToken(token);



		if (payload == null || payload.getId().equals("5")) 

		{

			status = Status.UNAUTHORIZED;

		} 

		

		else 

		{

			service.updateAssociate(afc);

			status = Status.OK;

		}

		

		return Response.status(status).build();

	}



	@PUT

	@ApiOperation(value = "updates associate values", notes = "The method updates start date of the client.")

	@Path("/{associateId}/{startDate}")

	public Response updateAssociate(@PathParam("associateId") Integer id, @PathParam("startDate") String startDate, @HeaderParam("Authorization") String token) 

	{

		Status status = null;

		Claims payload = JWTService.processToken(token);



		if (payload == null || payload.getId().equals("5")) 

		{

			status = Status.UNAUTHORIZED;

		} 

		

		else 

		{

			Session session = HibernateUtil.getSessionFactory().openSession();

			Transaction tx = session.beginTransaction();

			try 

			{

				StoredProcedureQuery spq = session.createStoredProcedureCall("admin.UPDATEASSOCIATECLIENTSTARTDATE");

				spq.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);

				spq.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);

				spq.setParameter(1, id);

				spq.setParameter(2, startDate);

				spq.execute();

			} 

			catch (Exception e) 

			{

				logger.error(e);

				session.flush();

				tx.rollback();

			} 

			finally 

			{

				AssociateDaoHibernate.getInstance().cacheAllAssociates();

				session.close();

			}

			

			status = Status.OK;

		}

		

		

		return Response.status(status).build();

	}



	@PUT

	@ApiOperation(value = "updates associate verification", notes = "The method sets the verfication status to Approved of a given associate by their id.")

	@Path("/{associateId}/verify")

	public Response updateAssociateVerification(@PathParam("associateId") Integer id) {

		service.updateAssociateVerification(id);

		return Response.ok().build();

		

	}

	

	@ApiOperation(value = "returns all interviews for associate", notes= "Gets a list of all interviews for a specific associate.")

	@Path("/{associateid}/interviews")

	public InterviewResource addAssociateInterview() 

	{

			return new InterviewResource();

	}

}