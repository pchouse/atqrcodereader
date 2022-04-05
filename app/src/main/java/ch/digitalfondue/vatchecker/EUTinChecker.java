/*
 * Copyright (c) 2022. Reflexão Estudos e Sistemas Informáticos, Lda.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package ch.digitalfondue.vatchecker;

import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A small utility for calling the TIN webservice. See https://ec.europa.eu/taxation_customs/tin/ .
 * <p>
 * The main entry points are {@link #doCheck(String, String)} and if more customization is needed {@link #doCheck(String, String, BiFunction)}.
 */
public class EUTinChecker {

    public static final String ENDPOINT = "https://ec.europa.eu/taxation_customs/tin/services/checkTinService";
    private final BiFunction<String, String, InputStream> documentFetcher;

    private static final Document BASE_DOCUMENT_TEMPLATE;
    private static final XPathExpression VALID_ELEMENT_MATCHER;
    private static final XPathExpression[] VALID_EXTRACTORS;

    static {
        String soapCallTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<checkTin xmlns=\"urn:ec.europa.eu:taxud:tin:services:checkTin:types\">" +
                "<countryCode></countryCode><tinNumber></tinNumber>" +
                "</checkTin>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        BASE_DOCUMENT_TEMPLATE = Utils.toDocument(new StringReader(soapCallTemplate));
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            VALID_ELEMENT_MATCHER = xPath.compile("//*[local-name()='checkTinResponse']");
            VALID_EXTRACTORS = new XPathExpression[]{
                    xPath.compile("//*[local-name()='checkTinResponse']/*[local-name()='validSyntax']"),
                    xPath.compile("//*[local-name()='checkTinResponse']/*[local-name()='validStructure']")
            };
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    public EUTinChecker() {
        this(Utils::doCall);
    }

    public EUTinChecker(BiFunction<String, String, InputStream> documentFetcher) {
        this.documentFetcher = documentFetcher;
    }

    /**
     * See {@link #doCheck(String, String)}.
     *
     * @param countryCode 2 character ISO country code. Note: Greece is EL, not GR.
     * @param tinNr       TIN number
     * @return the response, see {@link EUTinCheckResponse}
     */
    public EUTinCheckResponse check(String countryCode, String tinNr) {
        return doCheck(countryCode, tinNr, this.documentFetcher);
    }

    /**
     * Do a call to the EU tin checker web service.
     *
     * @param countryCode 2 character ISO country code. Note: Greece is EL, not GR.
     * @param tinNr       the tin number to check
     * @return the response, see {@link EUTinCheckResponse}
     */
    public static EUTinCheckResponse doCheck(String countryCode, String tinNr) {
        return doCheck(countryCode, tinNr, Utils::doCall);
    }

    /**
     * See {@link #doCheck(String, String)}. This method accept a documentFetcher if you need to customize the
     * http client.
     *
     * @param countryCode     2 character ISO country code. Note: Greece is EL, not GR.
     * @param tinNumber       TIN number
     * @param documentFetcher the function that, given the url of the web service and the body to post, return the resulting body as InputStream
     * @return the response, see {@link EUTinCheckResponse}
     */
    public static EUTinCheckResponse doCheck(String countryCode, String tinNumber, BiFunction<String, String, InputStream> documentFetcher) {
        Objects.requireNonNull(countryCode, "countryCode cannot be null");
        Objects.requireNonNull(tinNumber, "tinNumber cannot be null");
        Map<String, String> params = new HashMap<>();
        params.put("countryCode", countryCode);
        params.put("tinNumber", tinNumber);
        Utils.ExtractionResult res = Utils.doCallAndExtract(BASE_DOCUMENT_TEMPLATE, params, ENDPOINT, documentFetcher, VALID_ELEMENT_MATCHER, VALID_EXTRACTORS);
        if (res.validNode != null) {
            return new EUTinCheckResponse("true".equals(res.extracted.get(0)), "true".equals(res.extracted.get(1)), false, null);
        } else if (res.faultNode != null) {
            return new EUTinCheckResponse(false, false, true, new EUTinCheckResponse.Fault(res.extracted.get(0), res.extracted.get(1)));
        } else {
            return new EUTinCheckResponse(false, false, true, null); // should not enter here in theory
        }
    }

}
