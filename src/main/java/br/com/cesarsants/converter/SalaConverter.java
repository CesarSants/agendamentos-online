package br.com.cesarsants.converter;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;

import br.com.cesarsants.domain.Sala;

/**
 * @author cesarsants
 *
 */

@Named
@FacesConverter(value = "salaConverter", forClass = Sala.class)
public class SalaConverter implements Converter {

    private static final String key = "br.com.cesarsants.converter.SalaConverter";
    private static final String sessionKey = "br.com.cesarsants.converter.SalaConverter.session";
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        // Primeiro tenta buscar no viewMap
        Map<String, Object> viewMap = getViewMap(context);
        Sala sala = (Sala) viewMap.get(value);
        
        // Se não encontrar no viewMap, tenta buscar no sessionMap
        if (sala == null) {
            Map<String, Object> sessionMap = getSessionMap(context);
            sala = (Sala) sessionMap.get(value);
        }
        
        return sala;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object sala) {
        if (sala == null) {
            return null;
        }
        
        String id = ((Sala) sala).getId().toString();
        
        // Armazena tanto no viewMap quanto no sessionMap
        Map<String, Object> viewMap = getViewMap(context);
        Map<String, Object> sessionMap = getSessionMap(context);
        
        viewMap.put(id, sala);
        sessionMap.put(id, sala);
        
        return id;
    }
    
    private Map<String, Object> getViewMap(FacesContext context) {
        Map<String, Object> viewMap = context.getViewRoot().getViewMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> idMap = (Map<String, Object>) viewMap.get(key);
        if (idMap == null) {
            idMap = new HashMap<>();
            viewMap.put(key, idMap);
        }
        return idMap;
    }
    
    private Map<String, Object> getSessionMap(FacesContext context) {
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> idMap = (Map<String, Object>) sessionMap.get(sessionKey);
        if (idMap == null) {
            idMap = new HashMap<>();
            sessionMap.put(sessionKey, idMap);
        }
        return idMap;
    }
} 