/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.resource;

import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.handler.ClusterHandler;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;

import com.google.common.base.Optional;

import io.swagger.annotations.Api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/clusters")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Clusters {
    private static final Logger LOG = LoggerFactory.getLogger(Clusters.class);
    private final Authorizer authorizer;
    private final EnvironDAO environDAO;
    private final GroupDAO groupDAO;
    private final ClusterHandler clusterHandler;
    private final ConfigHistoryHandler configHistoryHandler;

    public Clusters(TeletraanServiceContext context) {
        authorizer = context.getAuthorizer();
        environDAO = context.getEnvironDAO();
        groupDAO = context.getGroupDAO();
        clusterHandler = new ClusterHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
    }

    @POST
    public void createCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName,
                              @Valid ClusterBean clusterBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        clusterBean.setCluster_name(clusterName);
        clusterHandler.createCluster(clusterBean);
        groupDAO.addGroupCapacity(envBean.getEnv_id(), clusterName);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully create cluster for %s/%s by %s", envName, stageName, operator));
    }

    @PUT
    public void updateCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName,
                              @Valid ClusterBean clusterBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        clusterBean.setCluster_name(clusterName);
        clusterHandler.updateCluster(clusterName, clusterBean);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully update cluster for %s/%s by %s", envName, stageName, operator));
    }

    @GET
    public ClusterBean getCluster(@PathParam("envName") String envName,
                                  @PathParam("stageName") String stageName) throws Exception {
        String clusterName = String.format("%s-%s", envName, stageName);
        return clusterHandler.getCluster(clusterName);
    }

    @DELETE
    public void deleteCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        clusterHandler.deleteCluster(clusterName);
        groupDAO.removeGroupCapacity(envBean.getEnv_id(), clusterName);

        String configChange = String.format("delete cluster for %s/%s", envName, stageName);
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_OTHER, configChange, operator);
        LOG.info(String.format("Successfully delete cluster for %s/%s by %s", envName, stageName, operator));
    }

    @PUT
    @Path("/hosts")
    public void launchHosts(@Context SecurityContext sc,
                            @PathParam("envName") String envName,
                            @PathParam("stageName") String stageName,
                            @QueryParam("num") Optional<Integer> num) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        clusterHandler.launchHosts(clusterName, num.or(1));

        String configChange = String.format("launch %d hosts", num.or(1));
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_HOST_LAUNCH, configChange, operator);
        LOG.info(String.format("Successfully launch %d hosts for %s/%s by %s", num.or(1), envName, stageName, operator));
    }

    @DELETE
    @Path("/hosts")
    public void terminateHosts(@Context SecurityContext sc,
                               @PathParam("envName") String envName,
                               @PathParam("stageName") String stageName,
                               @Valid Collection<String> hostIds,
                               @QueryParam("replaceHost") Optional<Boolean> replaceHost) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        clusterHandler.terminateHosts(clusterName, hostIds, replaceHost.or(true));

        String configChange = String.format("hostIds: %s", hostIds.toString());
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_HOST_TERMINATE, configChange, operator);
        LOG.info(String.format("Successfully terminate hosts for %s/%s by %s\nhostIds: %s", envName, stageName, operator, hostIds.toString()));
    }

    @GET
    @Path("/hosts")
    public Collection<String> getHosts(@PathParam("envName") String envName,
                                @PathParam("stageName") String stageName,
                                @Valid Collection<String> hostIds) throws Exception {
        String clusterName = String.format("%s-%s", envName, stageName);
        return clusterHandler.getHosts(clusterName, hostIds);
    }

    @POST
    @Path("/provider/AWS")
    public void createAdvancedAwsCluster(@Context SecurityContext sc,
                                         @PathParam("envName") String envName,
                                         @PathParam("stageName") String stageName,
                                         @Valid AwsVmBean awsVmBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        awsVmBean.setClusterName(clusterName);
        ClusterBean clusterBean = clusterHandler.createAwsVmCluster(awsVmBean);
        groupDAO.addGroupCapacity(envBean.getEnv_id(), clusterName);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully create advanced cluster for %s/%s by %s", envName, stageName, operator));
    }

    @PUT
    @Path("/provider/AWS")
    public void updateAdvancedAwsCluster(@Context SecurityContext sc,
                                         @PathParam("envName") String envName,
                                         @PathParam("stageName") String stageName,
                                         @Valid AwsVmBean awsVmBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String clusterName = String.format("%s-%s", envName, stageName);
        awsVmBean.setClusterName(clusterName);
        ClusterBean clusterBean = clusterHandler.updateAwsVmCluster(clusterName, awsVmBean);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully update advanced cluster for %s/%s by %s", envName, stageName, operator));
    }

    @GET
    @Path("/provider/AWS")
    public AwsVmBean getAdvancedAwsCluster(@PathParam("envName") String envName,
                                           @PathParam("stageName") String stageName) throws Exception {
        String clusterName = String.format("%s-%s", envName, stageName);
        return clusterHandler.getAwsVmCluster(clusterName);
    }
}
