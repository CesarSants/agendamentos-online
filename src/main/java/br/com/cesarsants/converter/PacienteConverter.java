package br.com.cesarsants.converter;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import br.com.cesarsants.domain.Paciente;

@FacesConverter(value = "pacienteConverter")
public class PacienteConverter implements Converter {

    private static final String key = "br.com.cesarsants.converter.PacienteConverter";
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        Map<String, Object> viewMap = getViewMap(context);
        return viewMap.get(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return null;
        }
        
        String id = ((Paciente) value).getId().toString();
        Map<String, Object> viewMap = getViewMap(context);
        viewMap.put(id, value);
        
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
}