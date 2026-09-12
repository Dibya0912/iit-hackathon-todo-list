package dev.levelforge;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;
@RestControllerAdvice
public class Errors {
    public record ErrorBody(String code,String message,Map<String,String> fields) {}
    @ExceptionHandler(ApiError.class) ResponseEntity<ErrorBody> api(ApiError e){return ResponseEntity.status(e.status).body(new ErrorBody(e.code,e.getMessage(),Map.of()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e){
        Map<String,String> fields=new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f->fields.put(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION","Check the highlighted fields.",fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,IllegalArgumentException.class})
    ResponseEntity<ErrorBody> invalid(Exception e){return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST","Check the supplied values.",Map.of()));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> conflict(){return ResponseEntity.status(409).body(new ErrorBody("CONFLICT","This change conflicts with an existing record.",Map.of()));}
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorBody> typeMismatch(){return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST","Check the filter, date, or record identifier.",Map.of()));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e){
        org.slf4j.LoggerFactory.getLogger(Errors.class).error("Request failed",e);
        return ResponseEntity.status(500).body(new ErrorBody("INTERNAL_ERROR","The server could not finish this request. Please retry.",Map.of()));
    }
}
