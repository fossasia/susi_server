it('should check ng-class-odd and ng-class-even', function() {
  expect(element(by.repeater('tweet in tweets').row(0).column('tweet')).getAttribute('class')).
    toMatch(/timeline-inverted/);
  expect(element(by.repeater('tweet in tweets').row(1).column('tweet')).getAttribute('class')).
    toMatch(/empty-class/);
});