package com.smartmove.backend.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.VehicleDocumentService;
import com.smartmove.backend.service.VehicleDocumentService.CreateVehicleDocumentRequest;
import com.smartmove.backend.service.VehicleDocumentService.VehicleDocumentProfile;
import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/vehicle-documents/files")
public class VehicleFileController {
    private final GridFsTemplate gridFs;
    private final VehicleDocumentService documents;
    private final CurrentUserService users;
    public VehicleFileController(GridFsTemplate gridFs, VehicleDocumentService documents, CurrentUserService users) {
        this.gridFs=gridFs; this.documents=documents; this.users=users;
    }

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VehicleDocumentProfile> upload(
            @RequestParam Long vehicleId,
            @RequestParam String documentType,
            @RequestParam String title,
            @RequestParam(required=false) String description,
            @RequestParam(required=false) LocalDate expiryDate,
            @RequestPart("file") MultipartFile file) throws IOException {
        users.requireAdmin();
        if(file.isEmpty() || file.getSize()>10L*1024*1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"File must be 1 byte to 10 MB");
        String type=file.getContentType();
        if(!("application/pdf".equals(type) || "image/jpeg".equals(type) || "image/png".equals(type) || "image/webp".equals(type)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PDF, JPEG, PNG and WebP files are supported");
        ObjectId id=gridFs.store(file.getInputStream(),file.getOriginalFilename(),type);
        try {
            var record=documents.createDocument(new CreateVehicleDocumentRequest(
                vehicleId,documentType,title,description,null,null,null,expiryDate,
                file.getOriginalFilename(),id.toHexString(),
                "/api/vehicle-documents/files/"+id.toHexString(),type,file.getSize(),
                null,true,30,null,null,users.getCurrentAccountId()));
            return ResponseEntity.status(HttpStatus.CREATED).body(record);
        } catch(RuntimeException ex) {
            gridFs.delete(org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)));
            throw ex;
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) throws IOException {
        users.requireAdmin();
        if(!ObjectId.isValid(fileId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        GridFSFile file=gridFs.findOne(org.springframework.data.mongodb.core.query.Query.query(
            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(new ObjectId(fileId))));
        if(file==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        GridFsResource resource=gridFs.getResource(file);
        String type=file.getMetadata()==null?"application/octet-stream":file.getMetadata().getString("_contentType");
        if(type==null) type="application/octet-stream";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
            .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\"vehicle-file\"")
            .contentLength(file.getLength()).body(new InputStreamResource(resource.getInputStream()));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> remove(@PathVariable String documentId) {
        users.requireAdmin();
        var record=documents.getDocumentById(documentId);
        documents.archiveDocument(documentId);
        if(record.storedFileName()!=null && ObjectId.isValid(record.storedFileName()))
            gridFs.delete(org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(new ObjectId(record.storedFileName()))));
        return ResponseEntity.noContent().build();
    }
}
