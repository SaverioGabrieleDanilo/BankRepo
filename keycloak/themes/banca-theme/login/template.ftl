<#--
  Basato su theme/base/login/template.ftl (Keycloak 24.0.1).
  Struttura aggiornata per corrispondere esattamente al design e alle classi del FE Angular,
  preservando intatta tutta la logica dinamica di Keycloak.
-->
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}"<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />

    <#-- Font icone per simboli Material -->
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" />

    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script src="${url.resourcesPath}/js/menu-button-links.js" type="module"></script>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if authenticationSession??>
        <script type="module">
            import { checkCookiesAndSetTimer } from "${url.resourcesPath}/js/authChecker.js";

            checkCookiesAndSetTimer(
              "${authenticationSession.authSessionId}",
              "${authenticationSession.tabId}",
              "${url.ssoLoginInOtherTabsUrl?no_esc}"
            );
        </script>
    </#if>
</head>

<body class="${properties.kcBodyClass!}">
<div class="${properties.kcLoginClass!}">

    <#-- Language Selector posizionato in alto a destra sopra il banner -->
    <#if realm.internationalizationEnabled && locale.supported?size gt 1>
        <div class="${properties.kcLocaleMainClass!}" id="kc-locale">
            <div id="kc-locale-wrapper" class="${properties.kcLocaleWrapperClass!}">
                <div id="kc-locale-dropdown" class="menu-button-links ${properties.kcLocaleDropDownClass!}">
                    <button tabindex="1" id="kc-current-locale-link" aria-label="${msg("languages")}" aria-haspopup="true" aria-expanded="false" aria-controls="language-switch1">${locale.current}</button>
                    <ul role="menu" tabindex="-1" aria-labelledby="kc-current-locale-link" aria-activedescendant="" id="language-switch1" class="${properties.kcLocaleListClass!}">
                        <#assign i = 1>
                        <#list locale.supported as l>
                            <li class="${properties.kcLocaleListItemClass!}" role="none">
                                <a role="menuitem" id="language-${i}" class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                            </li>
                            <#assign i++>
                        </#list>
                    </ul>
                </div>
            </div>
        </div>
    </#if>

    <#-- Header con Brand coerente col design Angular -->
    <header class="login-header">
        <#-- Pulsante Indietro Dinamico -->
        <a class="login-header__back" id="login-back-btn" href="http://localhost:4200/landing" role="button" aria-label="${msg("backButton")!'Back'}">
            <span class="material-symbols-outlined login-header__back-icon">arrow_back</span>
        </a>
        <script>
            document.addEventListener("DOMContentLoaded", function() {
                const params = new URLSearchParams(window.location.search);
                const redirectUri = params.get('redirect_uri');
                if (redirectUri) {
                    try {
                        const url = new URL(redirectUri);
                        // Estrae l'origine (es. http://localhost:4200 o https://tuodominio.com) e ci aggiunge /landing
                        document.getElementById('login-back-btn').href = url.origin + '/landing';
                    } catch (e) {
                        console.warn("Impossibile parsare il redirect_uri, uso il fallback di sviluppo.");
                    }
                }
            });
        </script>
        <div class="login-header__content">
            <div class="login-header__brand">
                <span class="material-symbols-outlined login-header__logo">account_balance</span>
                <h1 class="login-header__title">${msg("themeBrandTitle")!'Nexus Bank'}</h1>
            </div>
            <p class="login-header__subtitle">${msg("themeLoginSubtitle")!'Institutional security for your personal digital economy.'}</p>
        </div>
    </header>

    <#-- Main Content Container -->
    <main class="login-main">
        <div class="${properties.kcFormCardClass!} glass-panel">
            
            <header class="${properties.kcFormHeaderClass!}">
                <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                    <#if displayRequiredFields>
                        <div class="${properties.kcContentWrapperClass!}">
                            <div class="${properties.kcLabelWrapperClass!} subtitle">
                                <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                            </div>
                            <div class="col-md-10">
                                <h2 id="kc-page-title" class="login-card__title"><#nested "header"></h2>
                            </div>
                        </div>
                    <#else>
                        <h2 id="kc-page-title" class="login-card__title"><#nested "header"></h2>
                    </#if>
                <#else>
                    <#if displayRequiredFields>
                        <div class="${properties.kcContentWrapperClass!}">
                            <div class="${properties.kcLabelWrapperClass!} subtitle">
                                <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                            </div>
                            <div class="col-md-10">
                                <#nested "show-username">
                                <div id="kc-username" class="${properties.kcFormGroupClass!}">
                                    <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                                    <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                                        <div class="kc-login-tooltip">
                                            <i class="${properties.kcResetFlowIcon!}"></i>
                                            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                        </div>
                                    </a>
                                </div>
                            </div>
                        </div>
                    <#else>
                        <#nested "show-username">
                        <div id="kc-username" class="${properties.kcFormGroupClass!}">
                            <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                            <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                                <div class="kc-login-tooltip">
                                    <i class="${properties.kcResetFlowIcon!}"></i>
                                    <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                </div>
                            </a>
                        </div>
                    </#if>
                </#if>
            </header>

            <div id="kc-content">
                <div id="kc-content-wrapper">

                    <#-- Alert e feedback dinamici Keycloak rimpiazzati con lo stile dell'app -->
                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="login-alert alert-${message.type}">
                            <span class="material-symbols-outlined login-alert__icon">
                                <#if message.type = 'success'>check_circle<#elseif message.type = 'warning'>warning<#elseif message.type = 'error'>error<#else>info</#if>
                            </span>
                            <span class="login-alert__title">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#nested "form">

                    <#if auth?has_content && auth.showTryAnotherWayLink()>
                        <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                            <div class="${properties.kcFormGroupClass!}">
                                <input type="hidden" name="tryAnotherWay" value="on"/>
                                <a href="#" id="try-another-way"
                                   onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                            </div>
                        </form>
                    </#if>

                    <#nested "socialProviders">

                    <#if displayInfo>
                        <div id="kc-info" class="login-registration">
                            <div id="kc-info-wrapper">
                                <#nested "info">
                            </div>
                        </div>
                    </#if>
                </div>
            </div>

        </div>

        <#-- Trust Indicator fedele al FE Angular -->
        <div class="trust-indicator">
            <div class="trust-indicator__secure">
                <span class="material-symbols-outlined">verified_user</span>
                <span class="trust-indicator__secure-text">${msg("themeTrustSecureText")!'End-to-End Encrypted'}</span>
            </div>
            <div class="trust-indicator__divider"></div>
            <p class="trust-indicator__board">${msg("themeTrustBoardText")!'Member of the Global Financial Stability Board'}</p>
        </div>
    </main>

</div>
</body>
</html>
</#macro>