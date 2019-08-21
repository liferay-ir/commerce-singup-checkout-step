/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package ir.sain.commerce.checkout.step.internal;

import com.liferay.captcha.configuration.CaptchaConfiguration;
import com.liferay.captcha.util.CaptchaUtil;
import com.liferay.commerce.checkout.web.util.BaseCommerceCheckoutStep;
import com.liferay.commerce.checkout.web.util.CommerceCheckoutStep;
import com.liferay.frontend.taglib.servlet.taglib.util.JSPRenderer;
import com.liferay.portal.kernel.captcha.CaptchaConfigurationException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.security.auth.session.AuthenticatedSessionManager;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.util.PropsValues;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Marco Leo
 * @author Andrea Di Giorgi
 */
@Component(
        immediate = true,
        property = {
                "commerce.checkout.step.name=" + CommerceSingupCheckoutStep.NAME,
                "commerce.checkout.step.order:Integer=" + 0
        },
        service = CommerceCheckoutStep.class
)

public class CommerceSingupCheckoutStep
        extends BaseCommerceCheckoutStep {

    public static final String NAME = "sign-up-or-login";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isOrder() {
        return true;
    }

    @Override
    public boolean isSennaDisabled() {
        return true;
    }

    @Override
    public void processAction(
            ActionRequest actionRequest, ActionResponse actionResponse)
            throws Exception {
        CaptchaConfiguration captchaConfiguration =
                getCaptchaConfiguration();

        if (captchaConfiguration.createAccountCaptchaEnabled()) {
            CaptchaUtil.check(actionRequest);
        }

        addUser(actionRequest, actionResponse);
    }

    @Override
    public void render(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse)
            throws Exception {

        _jspRenderer.renderJSP(
                _servletContext, httpServletRequest, httpServletResponse,
                "/signup_step/view.jsp");
    }

    @Override
    public boolean showControls(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        return false;
    }

    @Override
    public boolean isVisible(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        ThemeDisplay themeDisplay = (ThemeDisplay) httpServletRequest.getAttribute(
                WebKeys.THEME_DISPLAY);
        return !themeDisplay.isSignedIn();
    }

    @Override
    public boolean isActive(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        ThemeDisplay themeDisplay = (ThemeDisplay) httpServletRequest.getAttribute(
                WebKeys.THEME_DISPLAY);
        return !themeDisplay.isSignedIn();
    }

    protected CaptchaConfiguration getCaptchaConfiguration()
            throws CaptchaConfigurationException {

        try {
            return _configurationProvider.getSystemConfiguration(
                    CaptchaConfiguration.class);
        } catch (Exception e) {
            throw new CaptchaConfigurationException(e);
        }
    }

    protected void addUser(
            ActionRequest actionRequest, ActionResponse actionResponse)
            throws Exception {

        HttpServletRequest request = _portal.getHttpServletRequest(
                actionRequest);

        HttpSession session = request.getSession();

        ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(
                WebKeys.THEME_DISPLAY);

        Company company = themeDisplay.getCompany();

        boolean autoPassword = true;
        String password1 = null;
        String password2 = null;
        boolean autoScreenName = false;
        String screenName = ParamUtil.getString(actionRequest, "screenName");
        String emailAddress = ParamUtil.getString(
                actionRequest, "emailAddress");
        long facebookId = ParamUtil.getLong(actionRequest, "facebookId");
        String openId = ParamUtil.getString(actionRequest, "openId");
        String languageId = LocaleUtil.toLanguageId(
                LocaleUtil.getSiteDefault());
        String firstName = ParamUtil.getString(actionRequest, "firstName");
        String middleName = ParamUtil.getString(actionRequest, "middleName");
        String lastName = ParamUtil.getString(actionRequest, "lastName");
        long prefixId = ParamUtil.getInteger(actionRequest, "prefixId");
        long suffixId = ParamUtil.getInteger(actionRequest, "suffixId");
        boolean male = ParamUtil.getBoolean(actionRequest, "male", true);
        int birthdayMonth = ParamUtil.getInteger(
                actionRequest, "birthdayMonth");
        int birthdayDay = ParamUtil.getInteger(actionRequest, "birthdayDay");
        int birthdayYear = ParamUtil.getInteger(actionRequest, "birthdayYear");
        String jobTitle = ParamUtil.getString(actionRequest, "jobTitle");
        long[] groupIds = null;
        long[] organizationIds = null;
        long[] roleIds = null;
        long[] userGroupIds = null;
        boolean sendEmail = true;

        ServiceContext serviceContext = ServiceContextFactory.getInstance(
                User.class.getName(), actionRequest);

        if (PropsValues.LOGIN_CREATE_ACCOUNT_ALLOW_CUSTOM_PASSWORD) {
            autoPassword = false;

            password1 = ParamUtil.getString(actionRequest, "password1");
            password2 = ParamUtil.getString(actionRequest, "password2");
        }

        boolean openIdPending = false;

        Boolean openIdLoginPending = (Boolean) session.getAttribute(
                WebKeys.OPEN_ID_LOGIN_PENDING);

        if ((openIdLoginPending != null) && openIdLoginPending.booleanValue() &&
                Validator.isNotNull(openId)) {

            sendEmail = false;
            openIdPending = true;
        }

        User user = _userService.addUserWithWorkflow(
                company.getCompanyId(), autoPassword, password1, password2,
                autoScreenName, screenName, emailAddress, facebookId, openId,
                LocaleUtil.fromLanguageId(languageId), firstName, middleName,
                lastName, prefixId, suffixId, male, birthdayMonth, birthdayDay,
                birthdayYear, jobTitle, groupIds, organizationIds, roleIds,
                userGroupIds, sendEmail, serviceContext);

        if (openIdPending) {
            session.setAttribute(
                    WebKeys.OPEN_ID_LOGIN, Long.valueOf(user.getUserId()));

            session.removeAttribute(WebKeys.OPEN_ID_LOGIN_PENDING);
        } else {

            // Session messages

            if (user.getStatus() == WorkflowConstants.STATUS_APPROVED) {
                SessionMessages.add(
                        request, "userAdded", user.getEmailAddress());
                SessionMessages.add(
                        request, "userAddedPassword",
                        user.getPasswordUnencrypted());
            } else {
                SessionMessages.add(
                        request, "userPending", user.getEmailAddress());
            }
        }

        // Send redirect

        sendRedirect(
                actionRequest, actionResponse, themeDisplay, user,
                user.getPasswordUnencrypted());
    }

    protected void sendRedirect(
            ActionRequest actionRequest, ActionResponse actionResponse,
            ThemeDisplay themeDisplay, User user, String password)
            throws Exception {

        String login = null;

        Company company = themeDisplay.getCompany();

        String authType = company.getAuthType();

        if (authType.equals(CompanyConstants.AUTH_TYPE_ID)) {
            login = String.valueOf(user.getUserId());
        } else if (authType.equals(CompanyConstants.AUTH_TYPE_SN)) {
            login = user.getScreenName();
        } else {
            login = user.getEmailAddress();
        }

        HttpServletRequest request = _portal.getHttpServletRequest(
                actionRequest);

//		String redirect = _portal.escapeRedirect(
//				ParamUtil.getString(actionRequest, "redirect"));
//

        PortletURL redirectURL = getRedirect(
                request, themeDisplay.getPlid(), user.getFullName());

        String redirect = redirectURL.toString();

        if (Validator.isNotNull(redirect)) {
            HttpServletResponse response = _portal.getHttpServletResponse(
                    actionResponse);

            _authenticatedSessionManager.login(
                    request, response, login, password, false, authType);
        } else {
            PortletURL loginURL = getLoginURL(
                    request, themeDisplay.getPlid());

            loginURL.setParameter("login", login);

            redirect = loginURL.toString();
        }


        if (!themeDisplay.isSignedIn()) {
            LiferayPortletResponse liferayPortletResponse =
                    _portal.getLiferayPortletResponse(actionResponse);

            String portletId = "com_liferay_login_web_portlet_LoginPortlet";

            PortletURL actionURL = liferayPortletResponse.createActionURL(
                    portletId);

            actionURL.setParameter(
                    ActionRequest.ACTION_NAME, "/login/login");
            actionURL.setParameter("redirect", redirect);

            actionRequest.setAttribute(
                    WebKeys.REDIRECT, actionURL.toString());

            return;
        }

    }

    public static PortletURL getLoginURL(HttpServletRequest request, long plid)
            throws PortletModeException, WindowStateException {

        PortletURL portletURL = PortletURLFactoryUtil.create(
                request, "com_liferay_login_web_portlet_LoginPortlet", plid, PortletRequest.RENDER_PHASE);

        portletURL.setParameter("saveLastPath", Boolean.FALSE.toString());
        portletURL.setParameter("mvcRenderCommandName", "/login/login");
        portletURL.setPortletMode(PortletMode.VIEW);
        portletURL.setWindowState(WindowState.MAXIMIZED);

        return portletURL;
    }


    public static PortletURL getRedirect(HttpServletRequest request, long plid, String fullName)
            throws PortletModeException {

        PortletURL portletURL = PortletURLFactoryUtil.create(
                request, "com_liferay_commerce_checkout_web_internal_portlet_CommerceCheckoutPortlet", plid, PortletRequest.RENDER_PHASE);

        portletURL.setParameter("saveLastPath", Boolean.FALSE.toString());
        portletURL.setParameter("name", fullName);
        portletURL.setPortletMode(PortletMode.VIEW);

        return portletURL;
    }

    @Reference(unbind = "-")
    protected void setUserService(UserService userService) {
        _userService = userService;
    }

    @Reference
    private ConfigurationProvider _configurationProvider;

    @Reference
    private JSPRenderer _jspRenderer;

    @Reference(target = "(osgi.web.symbolicname=ir.sain.commerce.checkout.step)")

    private ServletContext _servletContext;

    @Reference
    private Portal _portal;

    @Reference
    private AuthenticatedSessionManager _authenticatedSessionManager;

    private UserService _userService;

}