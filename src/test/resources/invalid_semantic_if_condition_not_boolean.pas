program IfConditionNotBoolean;
var
  x: integer;
begin
  x := 1;
  if x then
    WriteLn('bad')
  else
    WriteLn('ok');
end.
