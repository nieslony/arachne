/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.apiindex;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Entity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author claas
 */
@Route(value = "api-index")
@RolesAllowed("ADMIN")
public class ApiIndexView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ApiIndexView.class);

    enum JsonMode {
        READ, WRITE
    }

    public ApiIndexView(ApiIndexBean apiIndexBean) {
        String urlPath = RouteConfiguration.forApplicationScope()
                .getUrl(getClass());

        H1 header = new H1("API Index");
        header.addClassName(LumoUtility.TextColor.PRIMARY);
        add(header);
        H2 tocHeader = new H2("Table of Contents");
        tocHeader.addClassName(LumoUtility.TextColor.PRIMARY);
        tocHeader.setId("toc");
        add(tocHeader);

        UnorderedList toc = new UnorderedList();
        add(toc);
        H2 apiCallsHeader = new H2("API Calls");
        apiCallsHeader.addClassName(LumoUtility.TextColor.PRIMARY);
        add(apiCallsHeader);

        apiIndexBean.getMappings()
                .entrySet()
                .stream()
                .filter((entry) -> {
                    try {
                        String pattern = entry.getKey()
                                .getPathPatternsCondition()
                                .getFirstPattern()
                                .getPatternString();
                        return pattern.startsWith("/api") || pattern.startsWith("/setup");
                    } catch (NullPointerException ex) {
                        return false;
                    }
                })
                .sorted((entry1, entry2) -> {
                    var path1 = entry1.getKey()
                            .getPathPatternsCondition()
                            .getFirstPattern()
                            .getPatternString();
                    var path2 = entry2.getKey()
                            .getPathPatternsCondition()
                            .getFirstPattern()
                            .getPatternString();
                    return path1.compareTo(path2);
                })
                .forEach((entry) -> {
                    var key = entry.getKey();
                    String pattern = key
                            .getPathPatternsCondition()
                            .getFirstPattern()
                            .getPatternString();
                    String txt = pattern + " " + key.getMethodsCondition().toString();
                    String href = (pattern + key.getMethodsCondition().toString())
                            .replaceAll("[{\\[/]", "_")
                            .replaceAll("[}\\]]", "");
                    Anchor anchor = new Anchor(urlPath + "#" + href, txt);
                    toc.add(new ListItem(anchor));

                    var method = entry.getValue().getMethod();
                    Anchor toToc = new Anchor(
                            urlPath + "#toc",
                            "TOC"
                    );
                    H3 methodHeader = new H3(
                            new Text(txt + " "),
                            toToc
                    );
                    methodHeader.setId(href);
                    add(
                            methodHeader,
                            new DescriptionList(createMethodDetails(method))
                    );
                });
    }

    private Map<DescriptionList.Term, DescriptionList.Description>
            createMethodDetails(Method method) {
        Map<DescriptionList.Term, DescriptionList.Description> methodDetails = new LinkedHashMap<>();

        List<Div> requestParams = new LinkedList<>();
        List<Div> pathParams = new LinkedList<>();
        List<String> otherParams = new LinkedList<>();
        Div body = null;
        for (var param : method.getParameters()) {
            if (param.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }

            RequestParam requestParam = param.getDeclaredAnnotation(RequestParam.class);
            if (requestParam != null) {
                String name = requestParam.name();
                if (name.isEmpty()) {
                    name = requestParam.value();
                }
                if (name.isEmpty()) {
                    name = param.getName();
                }

                boolean isRequired = requestParam.required();

                String type = param.getType().getSimpleName();

                String defaultValue = "";
                if (!isRequired) {
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(
                                requestParam
                                        .defaultValue()
                                        .getBytes()
                        );
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        Object obj = ois.readObject();
                        defaultValue = (String) obj;
                    } catch (IOException | ClassNotFoundException ex) {
                        logger.error("Cannot get defaultValue: " + ex.getMessage());
                    }
                }

                Div div = new Div(
                        new Text("name: " + name),
                        new HtmlComponent("br"),
                        new Text("type: " + type),
                        new HtmlComponent("br"),
                        new Text("is required: " + (isRequired ? "yes" : "no")),
                        new HtmlComponent("br"),
                        new Text("default value: " + defaultValue)
                );
                requestParams.add(div);
                continue;
            }

            PathVariable pathVariable = param.getDeclaredAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String name = pathVariable.name();
                if (name.isEmpty()) {
                    name = pathVariable.value();
                }
                if (name.isEmpty()) {
                    name = param.getName();
                }

                boolean isRequired = pathVariable.required();

                String type = param.getType().getSimpleName();

                Div div = new Div(
                        new Text("name: " + name),
                        new HtmlComponent("br"),
                        new Text("type: " + type),
                        new HtmlComponent("br"),
                        new Text("is required: " + (isRequired ? "yes" : "no"))
                );
                pathParams.add(div);
                continue;
            }

            RequestBody requestBody = param.getDeclaredAnnotation(RequestBody.class);
            if (requestBody != null) {
                boolean isRequired = requestBody.required();
                body = new Div(
                        new Text("is required: " + (isRequired ? "yes" : "no")),
                        new HtmlComponent("br"),
                        getTypeInformation(
                                param.getType(),
                                JsonMode.WRITE
                        )
                );
                continue;
            }

            otherParams.add(param.toString() + " " + Arrays.toString(param.getDeclaredAnnotations()));
        }

        if (method.isAnnotationPresent(ApiMethodDescription.class)) {
            String doc = method
                    .getAnnotation(ApiMethodDescription.class)
                    .value();
            methodDetails.put(
                    new DescriptionList.Term("Description"),
                    new DescriptionList.Description(doc)
            );
        }
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            String[] rolesAllowed = method
                    .getAnnotation(RolesAllowed.class)
                    .value();
            methodDetails.put(
                    new DescriptionList.Term("Roles Allowed"),
                    new DescriptionList.Description(
                            Arrays.toString(rolesAllowed)
                    )
            );
        }
        if (!pathParams.isEmpty()) {
            UnorderedList l = new UnorderedList();
            pathParams.forEach((div) -> {
                l.add(new ListItem(div));
            });
            methodDetails.put(
                    new DescriptionList.Term("Path Parameters"),
                    new DescriptionList.Description(l)
            );
        }
        if (!requestParams.isEmpty()) {
            UnorderedList l = new UnorderedList();
            requestParams.forEach((div) -> {
                l.add(new ListItem(div));
            });
            methodDetails.put(
                    new DescriptionList.Term("Request Parameters"),
                    new DescriptionList.Description(l)
            );
        }
        if (body != null) {
            methodDetails.put(
                    new DescriptionList.Term("Request Body"),
                    new DescriptionList.Description(body)
            );
        }
        if (!otherParams.isEmpty()) {
            UnorderedList l = new UnorderedList();
            otherParams.forEach((div) -> {
                l.add(new ListItem(div));
            });
            methodDetails.put(
                    new DescriptionList.Term("Other Parameters"),
                    new DescriptionList.Description(l)
            );
        }
        var returnType = method.getGenericReturnType();
        if (returnType != void.class) {
            methodDetails.put(
                    new DescriptionList.Term("Returns"),
                    new DescriptionList.Description(
                            getTypeInformation(returnType, JsonMode.READ)
                    )
            );
        }

        return methodDetails;
    }

    public static List<String> getEnumNames(Class<?> c) {
        List<String> enumNames = new LinkedList<>();
        try {
            Method valuesMeth = c.getDeclaredMethod("values");
            Object result = valuesMeth.invoke(null);
            for (var o : ((Object[]) result)) {
                String name = ((Enum) o).name();
                enumNames.add(name);
            }
        } catch (Exception ex) {
            logger.error("Cannot get enum values: "
                    + ex.getClass().getName() + ": "
                    + ex.getMessage());
        }
        enumNames.sort(String::compareTo);

        return enumNames;
    }

    private Component getTypeInformation(Type returnType, JsonMode jsonMode) {
        if (returnType instanceof ParameterizedType pt) {
            if (pt.getRawType().getTypeName().equals(List.class.getName())
                    || pt.getRawType().getTypeName().equals(Set.class.getName())) {
                return new Span(
                        new Text("List of "),
                        getTypeInformation(pt.getActualTypeArguments()[0], jsonMode)
                );
            } else if (pt.getRawType().getTypeName().equals(Map.class.getName())) {
                return new Span(
                        new Text("Json map  of "),
                        getTypeInformation(pt.getActualTypeArguments()[0], jsonMode),
                        new Text(":"),
                        getTypeInformation(pt.getActualTypeArguments()[1], jsonMode)
                );
            } else {
                return new Text("Unknown parameterized type: " + pt.toString());
            }
        } else if (returnType instanceof Class<?> c) {
            if (AbstractSettingsGroup.class.isAssignableFrom(c)
                    || c.isAnnotationPresent(Entity.class)
                    || c.isAnnotationPresent(ShowApiDetails.class)) {
                return getJsonParams(c, jsonMode);
            } else if (c.isEnum()) {
                return new Text("Enum " + getEnumNames(c).toString());
            } else if (c.isAnnotationPresent(ShowApiType.class)) {
                ShowApiType showApiType = (ShowApiType) c.getAnnotation(ShowApiType.class);
                return getTypeInformation(showApiType.value(), jsonMode);
            } else {
                return new Text(c.getSimpleName());
            }
        } else {
            return new Text("simple name " + returnType.getTypeName());
        }
    }

    private Component getJsonParams(Class<?> c, JsonMode jsonMode) {
        Map<String, Component> items = new HashMap<>();

        for (var method : c.getDeclaredMethods()) {
            if (method.getAnnotation(JsonIgnore.class) != null) {
                continue;
            }
            if (Modifier.isPublic(method.getModifiers())) {
                String name = method.getName();
                String paramName;
                switch (jsonMode) {
                    case READ -> {
                        if (name.startsWith("get")) {
                            paramName = Character.toLowerCase(name.charAt(3))
                                    + name.substring(4);
                        } else if (name.startsWith("is") && method.getReturnType() == boolean.class) {
                            paramName = Character.toLowerCase(name.charAt(2))
                                    + name.substring(3);
                        } else {
                            continue;
                        }
                        items.put(
                                paramName,
                                getTypeInformation(method.getGenericReturnType(), jsonMode)
                        );
                    }
                    case WRITE -> {
                        if (name.startsWith("set") && method.getParameters().length == 1) {
                            paramName = Character.toLowerCase(name.charAt(3))
                                    + name.substring(4);
                        } else {
                            continue;
                        }
                        items.put(
                                paramName,
                                getTypeInformation(
                                        method.getParameters()[0].getType(),
                                        jsonMode)
                        );
                    }
                }
            }
        }

        UnorderedList ul = new UnorderedList();
        items
                .keySet()
                .stream()
                .sorted()
                .forEach((i) -> {
                    Div itemContent = new Div(
                            new Text(i + ": "),
                            items.get(i)
                    );
                    ul.add(new ListItem(itemContent));
                });
        return new Div(new Text("Json map"), ul);
    }
}
