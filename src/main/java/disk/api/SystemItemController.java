package disk.api;

import disk.SystemItem;
import disk.data.SystemItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import disk.SystemItem.Type;

import java.util.*;

@RestController
@RequestMapping(path = "/", produces = "application/json")
@CrossOrigin(origins = "http://localhost:8080")
public class SystemItemController {

    private final SystemItemRepository systemItemRepository;

    @Autowired
    public SystemItemController(SystemItemRepository systemItemRepository) {
        this.systemItemRepository = systemItemRepository;
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Error> deleteById(@PathVariable("id") String id){
        if(systemItemRepository.existsById(id)){
            systemItemRepository.deleteById(id);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } else if(!systemItemRepository.existsById(id)){
            return new ResponseEntity<>(new Error(404,"Item not found"),HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(new Error(400,"Validation Failed"),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/nodes/{id}")
    public ResponseEntity<SystemItem> itemById(@PathVariable("id") String id) {
        Optional<SystemItem> item = systemItemRepository.findById(id);
        if (item.isPresent()) {
            SystemItem systemItem = item.get();
            fixFile(systemItem);
            return new ResponseEntity<>(systemItem, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(path = "/imports",consumes = "application/json")
    public ResponseEntity<Error> postItems(@RequestBody SystemItemRequest requestBody) {
        ResponseEntity<Error> badResponse = new ResponseEntity<>(new Error(400, "Validation Failed"), HttpStatus.BAD_REQUEST);
        Set<String> usedId = new HashSet<>();
        List<SystemItem> items = requestBody.getItems();
        for (SystemItem systemItem : items){
            if (!usedId.contains(systemItem.getId())) {
                usedId.add(systemItem.getId());
            } else {
                return badResponse;
            }
        }
        for (SystemItem systemItem : items) {
            if (systemItem.getType() == Type.FILE) {
                if (!checkFile(systemItem,usedId)) {
                    return badResponse;
                }
            } else if (systemItem.getType() == Type.FOLDER) {
                if (!checkFolder(systemItem,usedId)) {
                    return badResponse;
                }
            }
        }
        int countOfSavings = -1;
        while(countOfSavings != 0){
            countOfSavings = 0;
            for (SystemItem item : items) {
                if (systemItemRepository.existsById(item.getId())) {
                    continue;
                }
                if (item.getParentId() == null) {
                    item.setDate(requestBody.getUpdateDate());
                    systemItemRepository.save(item);
                    if(item.getType() == Type.FILE) changeSize(item);
                    countOfSavings++;
                } else if (systemItemRepository.existsById(item.getParentId())) {
                    Optional<SystemItem> folder = systemItemRepository.findById(item.getParentId());
                    if (folder.isPresent()) {
                        SystemItem fl = folder.get();
                        if (folder.get().getChildren() != null) {
                            fl.getChildren().add(item);
                        } else {
                            fl.setChildren(new ArrayList<>());
                            fl.getChildren().add(item);
                        }
                        item.setDate(requestBody.getUpdateDate());
                        systemItemRepository.save(item);
                        systemItemRepository.save(fl);
                        if(item.getType() == Type.FILE) changeSize(item);
                        countOfSavings++;
                    }
                }
            }

        }
        return new ResponseEntity<>(null,HttpStatus.OK);
    }


    public boolean checkFile(SystemItem file, Set<String> usedId) {
        if(file.getParentId() != null){
            if(!systemItemRepository.existsById(file.getParentId()) && !usedId.contains(file.getParentId())){
                return false;
            }
            if(systemItemRepository.existsById(file.getParentId())){
                if(systemItemRepository.findById(file.getParentId()).get().getType() == Type.FILE){
                    return false;
                }
            }
        }
        return file.getId() != null && file.getSize() != null && file.getSize() > 0;
    }

    public boolean checkFolder(SystemItem folder,Set<String> usedId) {
        if(folder.getParentId() != null){
            if(!systemItemRepository.existsById(folder.getParentId()) && !usedId.contains(folder.getParentId())){
                return false;
            }
            if(systemItemRepository.existsById(folder.getParentId())){
                if(systemItemRepository.findById(folder.getParentId()).get().getType() == Type.FILE){
                    return false;
                }
            }
        }
        return folder.getId() != null && folder.getSize() == null;
    }

    public void changeSize(SystemItem item){
        int incSize = item.getSize();
        String updateDate = item.getDate();
        while(item.getParentId() != null){
            SystemItem folder = systemItemRepository.findById(item.getParentId()).get();
            folder.setDate(updateDate);
            if (folder.getSize() == null) {
                folder.setSize(incSize);
            } else {
                folder.setSize(folder.getSize() + incSize);
            }
            systemItemRepository.save(folder);
            item = folder;
        }
    }

    public void fixFile(SystemItem systemItem){
        if(systemItem.getType() == Type.FILE){
            systemItem.setChildren(null);
        } else {
            for(SystemItem item : systemItem.getChildren()){
                if(item.getType() == Type.FILE){
                    item.setChildren(null);
                } else {
                    fixFile(item);
                }
            }
        }

    }

}
