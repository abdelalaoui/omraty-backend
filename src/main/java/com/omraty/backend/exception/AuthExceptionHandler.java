package com.omraty.backend.exception;

import com.omraty.backend.dto.response.ErrorResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(AuthException.PhoneAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handlePhoneAlreadyUsed(
            AuthException.PhoneAlreadyUsedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({
        AuthException.InvalidCredentialsException.class,
        AuthException.InvalidRefreshTokenException.class,
        AuthException.InvalidTokenException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(AuthException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message =
                e.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                "Requête invalide : vérifiez le format et les valeurs envoyées"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Paramètre invalide : " + e.getName()));
    }

    @ExceptionHandler(UserException.InvalidIdentityRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIdentityRequest(
            UserException.InvalidIdentityRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(UserException.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserException.UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(UserException.IdentityNotPendingException.class)
    public ResponseEntity<ErrorResponse> handleIdentityNotPending(
            UserException.IdentityNotPendingException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BannerException.InvalidBannerRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBannerRequest(
            BannerException.InvalidBannerRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BannerException.BannerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBannerNotFound(
            BannerException.BannerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BenefitException.InvalidBenefitRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBenefitRequest(
            BenefitException.InvalidBenefitRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BenefitException.BenefitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBenefitNotFound(
            BenefitException.BenefitNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ServiceCardException.InvalidServiceCardRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidServiceCardRequest(
            ServiceCardException.InvalidServiceCardRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ServiceCardException.ServiceCardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServiceCardNotFound(
            ServiceCardException.ServiceCardNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(HotelException.InvalidHotelRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidHotelRequest(
            HotelException.InvalidHotelRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(HotelException.HotelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHotelNotFound(
            HotelException.HotelNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(VipRequestException.InvalidVipRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVipRequest(
            VipRequestException.InvalidVipRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(VipRequestException.VipRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVipRequestNotFound(
            VipRequestException.VipRequestNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(VipRequestException.VipRequestStateException.class)
    public ResponseEntity<ErrorResponse> handleVipRequestState(
            VipRequestException.VipRequestStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(PackageException.InvalidPackageRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPackageRequest(
            PackageException.InvalidPackageRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(PackageException.PackageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePackageNotFound(
            PackageException.PackageNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RoomException.InvalidRoomTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRoomType(
            RoomException.InvalidRoomTypeException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RoomException.GroupSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleGroupSizeExceeded(
            RoomException.GroupSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AgencyCodeException.InvalidAgencyCodeRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAgencyCodeRequest(
            AgencyCodeException.InvalidAgencyCodeRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AgencyCodeException.AgencyCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAgencyCodeNotFound(
            AgencyCodeException.AgencyCodeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AgencyCodeException.AgencyCodeAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleAgencyCodeAlreadyUsed(
            AgencyCodeException.AgencyCodeAlreadyUsedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Erreur inattendue", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Une erreur interne est survenue"));
    }
}
