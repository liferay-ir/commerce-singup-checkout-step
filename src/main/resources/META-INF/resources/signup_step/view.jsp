<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
--%>

<%@ include file="/init.jsp" %>

<div class="row">
    <div class="col-md-4">
        <h3 class="title">
            <liferay-ui:icon icon="signin"/>
            <liferay-ui:message key="sign-in"/>
        </h3>
        <liferay-util:include page="/navigation.jsp"  portletId="com_liferay_login_web_portlet_LoginPortlet">
            <portlet:param name="mvcRenderCommandName" value="/login/create_account" />
        </liferay-util:include>
    </div>

    <div class="col-md-8">
        <h3 class="title">
            <liferay-ui:icon iconCssClass="icon-large icon-user"/>
            <liferay-ui:message key="create-a-new-account-to-buy"/>
        </h3>
        <liferay-util:include page="/signup_step/create_account.jsp"  servletContext="<%=application%>">
            <portlet:param name="mvcRenderCommandName" value="/login/create_account" />
        </liferay-util:include>

    </div>

</div>


