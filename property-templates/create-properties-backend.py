#!/usr/bin/python3

import sys
import re
import json
import os
import argparse
import glob

entries = {}
    
def print_variables():
    s = ""
    
    for e in entries["properties"]:
        s += "    private %s %s;\n" % (e["type"], e["name"])
        
    return s
    
def print_enums_backend():
    s = ""
    
    if "enums" in entries:
        for e in entries["enums"]:
            s += """      public enum %(enum_type)s {
      %(values)s
      }
"""         % { 
                "enum_type": e["type"],
                "values": ", ".join(e["values"])
            }
    
    return s

def print_ext_enums_backend():
    s = ""
    
    if "ext_enums" in entries:
        for e in entries["ext_enums"]:
            consts = e["values"]
            
            s += """  public enum %(enum_type)s {
        %(values)s;

        final private String description;
    
        private %(enum_type)s(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
""" % {
       "enum_type": e["type"],    
       "values": ",\n        ".join("%s(\"%s\")" % (ent["const"], ent["description"]) for ent in consts)
    }

    return s
    
def print_getter_setter_backend():
    s = ""
    
    for e in entries["properties"]:
        if "storeBase64" in e:
            s += """
    public %(type)s get%(u_name)s() {
        String value = %(default)s;
        
        try {
            PropertyGroup pg = getPropertyGroup();
            if (pg != null) {
                value = pg.getProperty(%(p_name)s, %(default)s);
                if (!value.isEmpty())
                    value = new String(Base64.getDecoder().decode(value));
            }
        }
        catch (SQLException ex) {
            value = %(default)s;
        }
        
        return %(type)s.valueOf(value);
    }
    
    public void set%(u_name)s(%(type)s %(name)s) {
        if (%(name)s != null && !%(name)s.isEmpty()) {
            %(name)s = new String(Base64.getEncoder().encode(%(name)s.getBytes()));
        }
    
        try {
            getPropertyGroup().setProperty(%(p_name)s, String.valueOf(%(name)s));
        }
        catch (SQLException ex) {
        }
    }    
    
""" %  {
        "u_name": e["name"][0].upper() + e["name"][1:],
        "p_name": prop_name(e["name"]),
        "name": e["name"],
        "type": e["type"],
        "default": "\"%s\"" % e["default"]
    }
        else:
            default = e["default"]
            
            if default.endswith("()"):
                s += """
    abstract public %(type)s %(func)s;                
""" % {
        "type": e["type"],
        "func": default
    }
            
            s += """
    public %(type)s get%(u_name)s() {
        String value = %(default)s;
        
        try {
            PropertyGroup pg = getPropertyGroup();
            if (pg != null)
                value = pg.getProperty(%(p_name)s, %(default)s);
        }
        catch (SQLException ex) {
            value = %(default)s;
        }
        
        return %(type)s.valueOf(value);
    }
    
    public void set%(u_name)s(%(type)s %(name)s) {
        try {
            getPropertyGroup().setProperty(%(p_name)s, String.valueOf(%(name)s));
        }
        catch (SQLException ex) {
        }
    }    
    
""" %  {
        "u_name": e["name"][0].upper() + e["name"][1:],
        "p_name": prop_name(e["name"]),
        "name": e["name"],
        "type": e["type"],
        "default": default if default.endswith("()") else "\"%s\"" % e["default"]
    }
    
    return s

def prop_name(n):
    return "PROP_%s" % re.sub('([A-Z]+)', r'_\1', n).upper()

def print_prop_names():
    s = ""
    
    for e in entries["properties"]:
        s += "    static final String %s = \"%s\";\n" % (
            prop_name(e["name"]),
             e["name"]
             )
        
    return s

def print_backend_class_base():    
    return """
package %(package)s.base;

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import java.sql.SQLException;
import java.util.Base64;
%(imports)s

public abstract class %(class_name)sBase {
%(prop_names)s
    abstract protected PropertyGroup getPropertyGroup();

%(enums)s
%(ext_enums)s
%(getter_setter)s
}
""" % {
    "package": entries["backend_package"],
    "class_name": entries["className"],
    "prop_names": print_prop_names(),
    "variables": print_variables(),
    "getter_setter": print_getter_setter_backend(),
    "enums": print_enums_backend(),
    "ext_enums": print_ext_enums_backend()
    ,"imports": print_imports()
    }

def print_backend_class():
    return """
package %(package)s;

import %(package)s.base.%(class_name)sBase;
import at.nieslony.openvpnadmin.beans.PropertiesStorageBean;
import java.io.Serializable;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.logging.Logger;
import at.nieslony.databasepropertiesstorage.PropertyGroup;

@ManagedBean
@ApplicationScoped
public class %(class_name)s
    extends %(class_name)sBase
    implements Serializable    
{
    public %(class_name)s() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{propertiesStorage}")
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;
    
        try {
            return propertiesStorage.getGroup("%(prop_group)s", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group %(prop_group)s: %%s",
                ex.getMessage()));                
            if (ex.getNextException() != null) 
            logger.severe(String.format("Cannot get property group %(prop_group)s: %%s",
                ex.getNextException().getMessage()));                
        }
        
        return null;
    }
}
""" % {
    "package": entries["backend_package"],
    "class_name": entries["className"],
    "prop_group": entries["propertyGroup"],
    }

def print_getter_setter_edit():
    s = ""
    for e in entries["properties"]:
        s += """
    public %(type)s get%(u_name)s() {
        return %(name)s;
    }
    
    public void set%(u_name)s(%(type)s value) {
        %(name)s = value;
    }
"""  % {
        "u_name": e["name"][0].upper() + e["name"][1:],    
        "name": e["name"],
        "type": e["type"],
    }

    return s

def print_save_values():
    s = ""
    for e in entries["properties"]:
        s += """      backend.set%(name)s(%(value)s);
""" % {
        "value": e["name"],        
        "name": e["name"][0].upper() + e["name"][1:],
    }

    return s

def print_load_values():
    s = ""
    for e in entries["properties"]:
        s += """      %(name)s = backend.get%(u_name)s();
""" % {
        "u_name": e["name"][0].upper() + e["name"][1:],    
        "name": e["name"],
    }
    return s

def print_imports():
    s = ""
    if "imports" in entries:
        for e in entries["imports"]:
            s += "import %s;\n" % e
            
    return s

def print_import_enums():
    s = ""
    
    if "enums" in entries:
        for e in entries["enums"]:
            s += "import %(backend_package)s.base.%(class_name)sBase.%(enum_name)s;\n" % {
                    "backend_package": entries["backend_package"],
                    "enum_name": e["type"],
                    "class_name": entries["className"]
                }
            
    if "ext_enums" in entries:
        for e in entries["ext_enums"]:
            s += "import %(backend_package)s.base.%(class_name)sBase.%(enum_name)s;\n" % {
                    "backend_package": entries["backend_package"],
                    "enum_name": e["type"],
                    "class_name": entries["className"]        
                    }
    
    return s

def print_reset_defaults():
    s = ""
    
    for e in entries["properties"]:
        s += """      %(name)s = %(type)s.valueOf("%(defaults)s");
""" % {
                "name": e["name"],
                "defaults": e["default"],
                "type": e["type"],
            }
    
    return s

def print_edit_class_base():
    return """
package %(package)s.base;

import %(backend_package)s.base.%(class_name)sBase;
%(import_enums)s
%(imports)s

public class Edit%(class_name)sBase {
    %(class_name)sBase backend;
    
%(variables)s
%(getter_setter)s    

    protected void setBackend(%(class_name)sBase backend) {
        this.backend = backend;
    }

    protected void save() {
%(save_values)s
    }
    
    protected void load() {
%(load_values)s    
    }
    
    protected void resetDefaults() {
%(reset_defaults)s    
    }
}

""" % {
    "package": entries["edit_package"],
    "backend_package": entries["backend_package"],
    "variables": print_variables(),
    "getter_setter": print_getter_setter_edit(),
    "class_name": entries["className"],
    "save_values": print_save_values(),
    "load_values": print_load_values(),
    "import_enums": print_import_enums(),
    "reset_defaults": print_reset_defaults(),
    "imports": print_imports()
    }

def print_edit_class():
    return """
package %(package)s;

import %(package)s.base.Edit%(class_name)sBase;
import %(backend_package)s.%(class_name)s;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean
@ViewScoped
public class Edit%(class_name)s 
    extends Edit%(class_name)sBase
    implements Serializable
{
    public Edit%(class_name)s () {
    }

    @ManagedProperty(value = "#{%(l_class_name)s}")
    %(class_name)s %(l_class_name)s;

    @PostConstruct
    public void init() {
        setBackend(%(l_class_name)s);
        load();
    }
    
    public void onSave() {
        save();
        FacesContext.getCurrentInstance().addMessage(
                null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Info", "Settings saved."));
    }
    
    public void onReset() {
        load();
    }
    
    public void onResetToDefaults() {
        resetDefaults();
    }
    
    public void set%(class_name)s(%(class_name)s v) {
        %(l_class_name)s = v;
    }
}
""" % {
    "package": entries["edit_package"],
    "backend_package": entries["backend_package"],
    "class_name": entries["className"],
    "l_class_name": entries["className"][0].lower() + entries["className"][1:],
    }

def read_file(f):
    global entries
    
    entries = json.load(f)

def main():
    global entries

    parser = argparse.ArgumentParser()
    parser.add_argument("--input_dir", default=".")
    parser.add_argument("--dest_dir", default=".")
    parser.add_argument("--create_skel", action='store_true')
    args = parser.parse_args(sys.argv[1:])
    
    for fn in glob.glob("%s/*json" % args.input_dir):
        print("Processing %s..." % fn)
        inf = open(fn)
        read_file(inf)
        inf.close()

        # Create backend base class
        outdir = "%s/%s/base" % (
            args.dest_dir,
            entries["backend_package"].replace(".", "/")
            )
        if not os.path.exists(outdir):
            os.makedirs(outdir)

        out_fn = "%s/%sBase.java" % (outdir, entries["className"])
        print("  Writing %s ..." % out_fn)
        f = open(out_fn, "w")
        f.write(print_backend_class_base())
        f.close()
        
        # Create backend bean
        if args.create_skel:
            outdir = "%s/%s" % (
                args.dest_dir,
                entries["backend_package"].replace(".", "/")
                )
            out_fn = "%s/%s.java" % (outdir, entries["className"])
            if not os.path.exists(out_fn):
                print("  Writing %s ..." % out_fn)
                f = open(out_fn, "w")
                f.write(print_backend_class())
                f.close()
            else:
                print("  Skipping %s, already exists" % out_fn)

        # Create edit base class
        outdir = "%s/%s/base" % (
            args.dest_dir,
            entries["edit_package"].replace(".", "/")
            )
        if not os.path.exists(outdir):
            os.makedirs(outdir)
        out_fn = "%s/Edit%sBase.java" % (outdir, entries["className"])
        print("  Writing %s ..." % out_fn)
        f = open(out_fn, "w")
        f.write(print_edit_class_base())
        f.close()

        # Create edit bean
        if args.create_skel:
            outdir = "%s/%s" % (
                args.dest_dir,
                entries["edit_package"].replace(".", "/")
                )
            out_fn = "%s/Edit%s.java" % (outdir, entries["className"])
            if not os.path.exists(out_fn):
                print("  Writing %s ..." % out_fn)
                f = open(out_fn, "w")
                f.write(print_edit_class())
                f.close()
            else:
                print("  Skipping %s, already exists" % out_fn)

if __name__ == "__main__":
    main()
    
