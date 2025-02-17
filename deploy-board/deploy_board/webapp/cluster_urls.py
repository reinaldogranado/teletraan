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
from django.conf.urls import url
import cluster_view

urlpatterns = [
    url(r'^clouds/create_base_image/$', cluster_view.create_base_image),
    url(r'^clouds/baseimages/$', cluster_view.get_base_images),
    url(r'^clouds/get_image_names/$', cluster_view.get_image_names),
    url(r'^clouds/get_base_images/$', cluster_view.get_base_images_by_name),
    url(r'^clouds/get_base_image_info/$', cluster_view.get_base_image_info),

    url(r'^clouds/create_host_type/$', cluster_view.create_host_type),
    url(r'^clouds/hosttypes/$', cluster_view.get_host_types),
    url(r'^clouds/get_host_types/$', cluster_view.get_host_types_by_provider),
    url(r'^clouds/get_host_type_info/$', cluster_view.get_host_type_info),

    url(r'^clouds/create_security_zone/$', cluster_view.create_security_zone),
    url(r'^clouds/securityzones/$', cluster_view.get_security_zones),
    url(r'^clouds/get_security_zones/$', cluster_view.get_security_zones_by_provider),
    url(r'^clouds/get_security_zone_info/$', cluster_view.get_security_zone_info),

    url(r'^clouds/create_placement/$', cluster_view.create_placement),
    url(r'^clouds/placements/$', cluster_view.get_placements),
    url(r'^clouds/get_placements/$', cluster_view.get_placements_by_provider),
    url(r'^clouds/get_placement_infos/$', cluster_view.get_placement_infos),

    url(r'^clouds/get_advanced_config/$', cluster_view.get_advanced_cluster),

    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/clusters/$', cluster_view.get_cluster),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/get_basic_cluster/$', cluster_view.get_basic_cluster),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/create_cluster/$', cluster_view.create_cluster),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/update_cluster/$', cluster_view.update_cluster),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/delete_cluster/$', cluster_view.delete_cluster),

    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/launch_hosts/$', cluster_view.launch_hosts),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/terminate_hosts/$', cluster_view.terminate_hosts),
]