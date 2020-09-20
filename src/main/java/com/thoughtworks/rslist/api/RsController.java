package com.thoughtworks.rslist.api;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.exception.Error;
import com.thoughtworks.rslist.exception.RequestNotValidException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.service.RsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@Validated
public class RsController {
  @Autowired RsEventRepository rsEventRepository;
  @Autowired UserRepository userRepository;
  @Autowired RsService rsService;

  @GetMapping("/rs/list")
  public ResponseEntity<List<RsEventDto>> getRsEventListBetween(
      @RequestParam(required = false) Integer start, @RequestParam(required = false) Integer end) {
    List<RsEventDto> rsEventDtos = rsEventRepository.findAll();
    List<RsEventDto> sortRsEventDtos =  SortRsEvent(rsEventDtos);
    if (start == null || end == null) {
      return ResponseEntity.ok(sortRsEventDtos);
    }

    List<RsEventDto> resultRsEventDtos = new ArrayList<>();
    for (int i=start-1;i<end;i++){
      resultRsEventDtos.add(sortRsEventDtos.get(i));
    }


    return ResponseEntity.ok(resultRsEventDtos);
  }

  @GetMapping("/rs/{index}")
  public ResponseEntity<RsEvent> getRsEvent(@PathVariable int index) {
    List<RsEvent> rsEvents =
        rsEventRepository.findAll().stream()
            .map(
                item ->
                    RsEvent.builder()
                        .eventName(item.getEventName())
                        .keyword(item.getKeyword())
                        .userId(item.getId())
                        .voteNum(item.getVoteNum())
                        .build())
            .collect(Collectors.toList());
    if (index < 1 || index > rsEvents.size()) {
      throw new RequestNotValidException("invalid index");
    }
    return ResponseEntity.ok(rsEvents.get(index - 1));
  }

  @PostMapping("/rs/event")
  public ResponseEntity addRsEvent(@RequestBody @Valid RsEvent rsEvent) {
    Optional<UserDto> userDto = userRepository.findById(rsEvent.getUserId());
    if (!userDto.isPresent()) {
      return ResponseEntity.badRequest().build();
    }
    RsEventDto build =
        RsEventDto.builder()
            .keyword(rsEvent.getKeyword())
            .eventName(rsEvent.getEventName())
            .voteNum(0)
             .rank(0)
             .amount(0)
            .user(userDto.get())
            .build();
    rsEventRepository.save(build);
    return ResponseEntity.created(null).build();
  }

  @PostMapping("/rs/vote/{id}")
  public ResponseEntity vote(@PathVariable int id, @RequestBody Vote vote) {
    rsService.vote(vote, id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/rs/buy/{id}")
  public ResponseEntity buy(@PathVariable int id, @RequestBody Trade trade){
    rsService.buy(trade, id);
    return ResponseEntity.ok().build();
  }


  @ExceptionHandler(RequestNotValidException.class)
  public ResponseEntity<Error> handleRequestErrorHandler(RequestNotValidException e) {
    Error error = new Error();
    error.setError(e.getMessage());
    return ResponseEntity.badRequest().body(error);
  }

  public List<RsEventDto> SortRsEvent(List<RsEventDto> rsEventDtos){
    List<RsEventDto> sortRsEventDot = new ArrayList<>();
    Collections.sort(rsEventDtos,new Comparator<RsEventDto>(){
      @Override
      public int compare(RsEventDto rsEventDto1,RsEventDto rsEventDto2){
        if(rsEventDto1.getVoteNum() >= rsEventDto2.getVoteNum()){
          return -1;
        }else {
          return 1;
        }
      }
    });
    for(int i=0;i<rsEventDtos.size();i++){
      if(rsEventDtos.get(i).getRank()!=0 && rsEventDtos.get(i).getRank()<(i+1)){
        RsEventDto changeRsEventDto = rsEventDtos.get(i);
        for(int j=i;j>=(rsEventDtos.get(i).getRank()-1);j--){
          rsEventDtos.get(j).builder().eventName(rsEventDtos.get(j-1).getEventName())
                  .keyword(rsEventDtos.get(j-1).getKeyword())
                  .amount(rsEventDtos.get(j-1).getAmount())
                  .rank(rsEventDtos.get(j-1).getRank())
                  .voteNum(rsEventDtos.get(j-1).getVoteNum())
                  .id(rsEventDtos.get(j-1).getId())
                  .user(rsEventDtos.get(j-1).getUser())
                  .tradeDtos(rsEventDtos.get(j-1).getTradeDtos())
                  .build();

        }
        rsEventDtos.get(changeRsEventDto.getRank()-1).builder().eventName(changeRsEventDto.getEventName())
                .keyword(changeRsEventDto.getKeyword())
                .amount(changeRsEventDto.getAmount())
                .rank(changeRsEventDto.getRank())
                .voteNum(changeRsEventDto.getVoteNum())
                .id(changeRsEventDto.getId())
                .user(changeRsEventDto.getUser())
                .tradeDtos(changeRsEventDto.getTradeDtos())
                .build();

      }
    }
    return rsEventDtos;
  }
}
