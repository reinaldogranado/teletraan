# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#  
#     http://www.apache.org/licenses/LICENSE-2.0
#    
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -*- coding: utf-8 -*-
"""Collection of all env promote config views
"""
import json
from deploy_board.settings import IS_PINTEREST
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
import common
from helpers import environs_helper, clusters_helper


class EnvConfigView(View):
    def get(self, request, name, stage):
        if request.is_ajax():
            env = environs_helper.get_env_by_stage(request, name, stage)
            html = render_to_string('configs/env_config.tmpl', {
                "env": env,
                "csrf_token": get_token(request),
            })
            return HttpResponse(json.dumps({'html': html}), content_type="application/json")

        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)

        # get capacity to decide if we need to show the remove stage button
        show_remove = True
        # if people have already specified host capacity or group capacity but do not have cluster config
        # show capacity config page; otherwise, show cluster config page
        show_cluster_config = True
        hosts = environs_helper.get_env_capacity(request, name, stage, capacity_type="HOST")
        if hosts:
            show_remove = False
            show_cluster_config = False
        else:
            groups = environs_helper.get_env_capacity(request, name, stage, capacity_type="GROUP")
            if groups:
                show_remove = False
                basic_cluster_info = clusters_helper.get_cluster(request, name, stage)
                if not basic_cluster_info:
                    show_cluster_config = False

        return render(request, 'configs/env_config.html', {
            "env": env,
            "stages": stages,
            "show_remove": show_remove,
            "show_cluster_config": show_cluster_config,
            "pinterest": IS_PINTEREST,
        })

    def post(self, request, name, stage):
        query_dict = request.POST
        data = {}
        if "notify_author" in query_dict:
            data["notifyAuthors"] = True
        else:
            data["notifyAuthors"] = False
        data["maxParallel"] = int(query_dict["maxParallelHosts"])
        data["priority"] = query_dict["priority"]
        data["stuckThreshold"] = int(query_dict["stuckThreshold"])
        data["successThreshold"] = float(query_dict["successThreshold"]) * 100
        data["description"] = query_dict.get("description")
        data["acceptanceType"] = query_dict["acceptanceType"]
        data["chatroom"] = query_dict.get("chatroom")
        data["buildName"] = query_dict["buildName"]
        data["branch"] = query_dict["branch"]
        data["emailRecipients"] = query_dict["email_recipients"]
        data["watchRecipients"] = query_dict["watch_recipients"]
        data["maxDeployNum"] = int(query_dict["maxDeployNum"])
        data["maxDeployDay"] = int(query_dict["maxDeployDay"])

        environs_helper.update_env_basic_config(request, name, stage, data=data)
        return self.get(request, name, stage)
