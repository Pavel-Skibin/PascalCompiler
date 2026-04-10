program InvalidSemantic;
var
  x: integer;
  flag: boolean;
begin
  x := true;
  y := 10;

  if x then
    WriteLn('bad');

  break;

  flag := x + 3;
end.
