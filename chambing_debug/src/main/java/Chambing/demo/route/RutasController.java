package Chambing.demo.route;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import Chambing.demo.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class RutasController {

    private final UsuarioRepository usuarios;

    public RutasController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    private String areaUsuario(String vista) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return vista;
        var usuario = usuarios.findByEmail(auth.getName()).orElse(null);
        if (usuario != null && !usuario.isPerfilCompleto()) return "redirect:/user-begin?completar=1";
        return vista;
    }

    
    @GetMapping("/")
    public String index() {
        return "index";
    }

    
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    
    @GetMapping("/user-dashboard")
    public String userDashboard() {
        return areaUsuario("user-dashboard");
    }

    @GetMapping("/user-begin")
    public String userBegin() {
        return "user-begin";
    }

    
    @GetMapping("/company-dashboard")
    public String companyDashboard() {
        return "company-dashboard";
    }

    @GetMapping("/companies-registers")
    public String companiesRegisters() {
        return areaUsuario("companies-registers");
    }

    @GetMapping("/only-proporsals")
    public String onlyProporsals() {
        return areaUsuario("only-proporsals");
    }

    @GetMapping("/proposals-answer")
    public String proposalsAnswer() {
        return "proposals-answer";
    }

    @GetMapping("/proposals-states")
    public String proposalsStates() {
        return areaUsuario("proposals-states");
    }


     @GetMapping("/companies-personals-profiles")
    public String companiesPersonalsProfiles() {
        return areaUsuario("companies-personals-profiles");
    }

    
    @GetMapping("/company-profile")
    public String companyProfile() {
        return "company-profile";
    }

    @GetMapping("/edit-propuesta")
    public String editPropuesta() {
        return "edit-propuesta";
    }

    @GetMapping("/company-proposal-edit")
    public String companyProposalEdit() {
        return "company-proposal-edit";
    }
    
    @GetMapping("/company-proposals")
    public String companyProposals() {
        return "company-proposals";
    }

  
    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    
    @GetMapping("/blog")
    public String blog() {
        return "blog";
    }

    
    @GetMapping("/about")
    public String about() {
        return "about";
    }

   
    @GetMapping({"/admin", "/admin/"})
    public String loginPage() {
        return "login-admin";
    }

    @GetMapping({"/admin/panel", "/admin/panel/"})
    public ModelAndView panelPage(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminLogged"))) {
            return new ModelAndView("redirect:/admin");
        }

        return new ModelAndView("panel");
    }

    @GetMapping("/403")
    public String accesoDenegado() {
        return "403";
    }

    @GetMapping("/error")
    public String errorGeneral() {
        return "error";
    }
}
