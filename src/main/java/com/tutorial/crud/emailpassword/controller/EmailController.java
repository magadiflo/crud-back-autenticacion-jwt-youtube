package com.tutorial.crud.emailpassword.controller;
import com.tutorial.crud.dto.Mensaje;
import com.tutorial.crud.emailpassword.dto.EmailValuesDTO;
import com.tutorial.crud.emailpassword.service.EmailService;

import com.tutorial.crud.security.entity.Usuario;
import com.tutorial.crud.security.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/email-password")
@CrossOrigin
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioService usuarioService;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${mail.subject}")
    private String subject;

    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmailTemplate(@RequestBody EmailValuesDTO dto){

        Optional<Usuario> usuarioOptional = this.usuarioService.getByNombreUsuarioOrEmail(dto.getMailTo());

        if(!usuarioOptional.isPresent()){
            return new ResponseEntity<>(new Mensaje("No existe ningún usuario con esas credenciales"), HttpStatus.NOT_FOUND);
        }

        Usuario usuario = usuarioOptional.get();
        dto.setMailFrom(this.mailFrom);
        dto.setMailTo(usuario.getEmail());
        dto.setUsername(usuario.getNombreUsuario());
        dto.setSubject(this.subject);

        String tokenPassword = this.getTokenPassword();

        dto.setTokenPassword(tokenPassword);
        usuario.setTokenPassword(tokenPassword);

        this.usuarioService.save(usuario);
        this.emailService.sendEmail(dto);

        return new ResponseEntity(new Mensaje("Te hemos enviado un correo. Por favor revísalo."), HttpStatus.OK);
    }

    private String getTokenPassword() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
