package com.tutorial.crud.emailpassword.controller;

import com.tutorial.crud.dto.Mensaje;
import com.tutorial.crud.emailpassword.dto.ChangePasswordDTO;
import com.tutorial.crud.emailpassword.dto.EmailValuesDTO;
import com.tutorial.crud.emailpassword.service.EmailService;

import com.tutorial.crud.security.entity.Usuario;
import com.tutorial.crud.security.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${mail.subject}")
    private String subject;

    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmailTemplate(@RequestBody EmailValuesDTO dto) {

        Optional<Usuario> usuarioOptional = this.usuarioService.getByNombreUsuarioOrEmail(dto.getMailTo());

        if (!usuarioOptional.isPresent()) {
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

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto, BindingResult bindingResult) {
        if(bindingResult.hasErrors()){
            return new ResponseEntity("Campos mal puestos", HttpStatus.BAD_REQUEST);
        }
        if(!dto.getPassword().equals(dto.getPasswordConfirm())){
            return new ResponseEntity("Las contraseñas no coinciden", HttpStatus.BAD_REQUEST);
        }
        Optional<Usuario> optionalUsuario = this.usuarioService.getByTokenPassword(dto.getTokenPassword());
        if (!optionalUsuario.isPresent()) {
            return new ResponseEntity(new Mensaje("No existe ningún usuario con esas credenciales"), HttpStatus.NOT_FOUND);
        }
        Usuario usuario = optionalUsuario.get();
        String newPassword = this.passwordEncoder.encode(dto.getPassword());
        usuario.setPassword(newPassword);
        usuario.setTokenPassword(null);

        this.usuarioService.save(usuario);
        return new ResponseEntity(new Mensaje("Contraseña actualizada"), HttpStatus.OK);
    }

    private String getTokenPassword() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
